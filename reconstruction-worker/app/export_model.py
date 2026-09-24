import bpy
import os
import sys

argv = sys.argv
if "--" not in argv:
    raise SystemExit("Missing export arguments")

args = argv[argv.index("--") + 1:]
if len(args) != 4:
    raise SystemExit("Expected: textured_mesh.ply texture.png output.glb output.usdz")

mesh_path, texture_path, glb_path, usdz_path = args

# Blender's PLY importer has changed namespace across releases.
if hasattr(bpy.ops.wm, "ply_import"):
    bpy.ops.wm.ply_import(filepath=mesh_path)
elif hasattr(bpy.ops.import_mesh, "ply"):
    bpy.ops.import_mesh.ply(filepath=mesh_path)
else:
    raise SystemExit("This Blender build does not provide a PLY importer")

obj = bpy.context.active_object
if obj is None:
    raise SystemExit("PLY import produced no active object")

obj.select_set(True)
bpy.context.view_layer.objects.active = obj

material = bpy.data.materials.new("FoodMaterial")
material.use_nodes = True
nodes = material.node_tree.nodes
links = material.node_tree.links
nodes.clear()

output = nodes.new("ShaderNodeOutputMaterial")
principled = nodes.new("ShaderNodeBsdfPrincipled")
tex = nodes.new("ShaderNodeTexImage")
tex.image = bpy.data.images.load(texture_path)

links.new(tex.outputs["Color"], principled.inputs["Base Color"])
links.new(principled.outputs["BSDF"], output.inputs["Surface"])

obj.data.materials.clear()
obj.data.materials.append(material)

# Export a web/mobile-friendly binary glTF.
bpy.ops.export_scene.gltf(
    filepath=glb_path,
    export_format="GLB",
    use_selection=True,
    export_apply=True,
    export_image_format="JPEG",
    export_texcoords=True,
    export_normals=True,
)

# Blender can export USDZ by using the .usdz extension.
bpy.ops.wm.usd_export(
    filepath=usdz_path,
    selected_objects_only=True,
    export_materials=True,
)

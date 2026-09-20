package com.webar.app.service;

import com.webar.app.dto.RestaurantRequest;
import com.webar.app.entity.Restaurant;
import com.webar.app.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    public Restaurant create(RestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .country(request.country())
                .website(request.website())
                .description(request.description())
                .active(true)
                .build();
        return repository.save(restaurant);
    }

    public List<Restaurant> getAll() {
        return repository.findAll();
    }

    public Restaurant getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));
    }

    public Restaurant update(Long id, RestaurantRequest request) {
        Restaurant restaurant = getById(id);
        restaurant.setName(request.name());
        restaurant.setPhone(request.phone());
        restaurant.setEmail(request.email());
        restaurant.setAddress(request.address());
        restaurant.setCity(request.city());
        restaurant.setState(request.state());
        restaurant.setCountry(request.country());
        restaurant.setWebsite(request.website());
        restaurant.setDescription(request.description());
        return repository.save(restaurant);
    }

    public Restaurant setActive(Long id, boolean active) {
        Restaurant restaurant = getById(id);
        restaurant.setActive(active);
        return repository.save(restaurant);
    }

    public Restaurant save(Restaurant restaurant) {
        return repository.save(restaurant);
    }
}

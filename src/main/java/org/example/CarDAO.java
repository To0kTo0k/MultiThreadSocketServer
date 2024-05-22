package org.example;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CarDAO {
    private final Map<Long, String> cars = new HashMap<>();

    @PostConstruct
    public void init() {
        cars.put(1000L, "BMW");
        cars.put(2000L, "Honda");
        cars.put(3000L, "Dodge");
        cars.put(4000L, "Lada");
        cars.put(5000L, "Rover");
    }

    public String getCar(Long number) {
        String car = cars.get(number);
        if (car == null) {
            car = "Марка авто под данным номером не найдена";
        }
        return car;
    }
}

package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

@RestController
public class MealsRestRpcStyleController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestRpcStyleController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @GetMapping("/restrpc/meals/{id}")
    Meal getMealById(@PathVariable String id) {
        Optional<Meal> meal = mealsRepository.findMeal(id);

        return meal.orElseThrow(() -> new MealNotFoundException(id));
    }

    @GetMapping("/restrpc/meals")
    Collection<Meal> getMeals() {
        return mealsRepository.getAllMeal();
    }

    @PostMapping("/restrpc/meals")
    void addMeal(@RequestBody Meal meal) { // annotation zegt: in de body staat iets dat je kan invullen in meal
        mealsRepository.addMeal(meal);
    }

    @PutMapping("/restrpc/meals/{id}")
    void updateMeal(@PathVariable String id, @RequestBody Meal meal) { // annotation zegt: in de body staat iets dat je kan invullen in meal
        meal.setId(id);
        mealsRepository.updateMeal(meal);
    }

    @DeleteMapping("/restrpc/meals/{id}")
    void deleteMeal(@PathVariable String id) { // annotation zegt: in de body staat iets dat je kan invullen in meal
        mealsRepository.deleteMeal(id);
    }

}

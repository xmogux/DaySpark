package com.dayspark.app.controller;

import com.dayspark.app.service.DareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The focus-timer page. The timer itself talks to {@link FocusApi}. */
@Controller
public class FocusController {

    private final DareService dares;

    public FocusController(DareService dares) {
        this.dares = dares;
    }

    @GetMapping("/focus")
    public String focus(@RequestParam(required = false) Long dareId, Model model) {
        model.addAttribute("dares", dares.active());
        model.addAttribute("selectedDareId", dareId);
        return "focus";
    }
}

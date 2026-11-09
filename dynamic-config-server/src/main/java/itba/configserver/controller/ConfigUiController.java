package itba.configserver.controller;

import itba.configserver.persistence.Property;
import itba.configserver.persistence.PropertyRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfigUiController {
    static final String APPLICATION = "fieldops";
    static final String PROFILE = "default";
    static final String LABEL = "main";

    private final PropertyRepository propertyRepository;

    public ConfigUiController(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("newProperty", new Property(APPLICATION, PROFILE, LABEL, "", ""));
        model.addAttribute("properties", propertyRepository.findByApplication(APPLICATION));
        model.addAttribute("knownProperties", KnownProperty.fieldOps());
        return "dashboard";
    }

    @PostMapping("/save")
    public String saveProperty(Property property, RedirectAttributes redirectAttributes) {
        property.setApplication(APPLICATION);
        property.setProfile(PROFILE);
        property.setLabel(LABEL);
        var existing = propertyRepository.findByApplicationAndProfileAndLabelAndKey(
                APPLICATION, PROFILE, LABEL, property.getKey());
        if (existing.isPresent()) {
            Property stored = existing.get();
            stored.setValue(property.getValue());
            propertyRepository.save(stored);
        } else {
            propertyRepository.save(property);
        }
        redirectAttributes.addFlashAttribute("success", "Propiedad guardada.");
        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteProperty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            propertyRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Propiedad quitada.");
        } catch (Exception _) {
            redirectAttributes.addFlashAttribute("error", "No se pudo quitar la propiedad.");
        }
        return "redirect:/";
    }
}

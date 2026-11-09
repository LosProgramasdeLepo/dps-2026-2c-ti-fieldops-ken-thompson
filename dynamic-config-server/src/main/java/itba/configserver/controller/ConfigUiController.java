package itba.configserver.controller;

import itba.configserver.persistence.Property;
import itba.configserver.persistence.PropertyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Controller
public class ConfigUiController {
    static final String APPLICATION = "fieldops";
    static final String PROFILE = "default";
    static final String LABEL = "main";

    private final PropertyRepository propertyRepository;
    private final HttpClient http = HttpClient.newHttpClient();
    private final URI refreshUri;

    public ConfigUiController(PropertyRepository propertyRepository,
            @Value("${FIELDOPS_API:http://localhost:8080}") String backend) {
        this.propertyRepository = propertyRepository;
        this.refreshUri = URI.create(trimSlash(backend) + "/actuator/refresh");
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

    @PostMapping("/refresh-client")
    public String refreshClient(RedirectAttributes redirectAttributes) {
        HttpRequest request = HttpRequest.newBuilder(refreshUri).POST(HttpRequest.BodyPublishers.noBody()).build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                redirectAttributes.addFlashAttribute("success", refreshMessage(response.body()));
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Se esperaba HTTP 2xx de POST " + refreshUri + ", se recibió " + response.statusCode() + ".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            redirectAttributes.addFlashAttribute("error", "El refresh al backend se interrumpió.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo POST " + refreshUri + ": " + e.getMessage() + ".");
        }
        return "redirect:/";
    }

    private static String trimSlash(String backend) {
        return backend.endsWith("/") ? backend.substring(0, backend.length() - 1) : backend;
    }

    private static String refreshMessage(String body) {
        if (body == null || body.isBlank() || "[]".equals(body.trim())) {
            return "Backend actualizado: ninguna clave cambió.";
        }
        return "Backend actualizado: " + body;
    }
}

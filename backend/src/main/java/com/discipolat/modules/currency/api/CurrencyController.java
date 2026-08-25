package com.discipolat.modules.currency.api;

import com.discipolat.modules.currency.domain.CurrencyConfig;
import com.discipolat.modules.currency.domain.CurrencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/currencies")
@PreAuthorize("isAuthenticated()")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(currencyService.listCurrencies());
    }

    @GetMapping("/primary")
    public ResponseEntity<?> getPrimary() {
        return ResponseEntity.ok(currencyService.getPrimaryCurrency());
    }

    @GetMapping("/supported")
    public ResponseEntity<?> getSupported() {
        return ResponseEntity.ok(currencyService.getSupportedCurrencies());
    }

    @GetMapping("/timezones")
    public ResponseEntity<?> getTimezones() {
        return ResponseEntity.ok(currencyService.getSupportedTimezones());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CurrencyConfig config) {
        return ResponseEntity.status(HttpStatus.CREATED).body(currencyService.create(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CurrencyConfig config) {
        return ResponseEntity.ok(currencyService.update(id, config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        currencyService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Devise supprimée"));
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convert(@RequestBody Map<String, Object> body) {
        double amount = ((Number) body.get("amount")).doubleValue();
        String from = (String) body.get("from");
        String to = (String) body.get("to");
        Double result = currencyService.convertAmount(amount, from, to);
        return ResponseEntity.ok(Map.of("amount", amount, "from", from, "to", to, "result", result));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(currencyService.getStats());
    }
}

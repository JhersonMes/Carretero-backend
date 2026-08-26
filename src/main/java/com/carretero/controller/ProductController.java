package com.carretero.controller;

import com.carretero.dto.PriceHistoryDTO;
import com.carretero.dto.ProductDTO;
import com.carretero.model.Product;
import com.carretero.model.User;
import com.carretero.model.enums.KitchenStation;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService service;
    private final IUserRepository userRepo;
    @Qualifier("productMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> findAll() {
        List<ProductDTO> list = service.findActive().stream()
                .map(e -> modelMapper.map(e, ProductDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> findAllRaw() throws Exception {
        List<ProductDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, ProductDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/category/{idCategory}")
    public ResponseEntity<List<ProductDTO>> findByCategory(@PathVariable("idCategory") Integer idCategory) {
        List<ProductDTO> list = service.findByCategoryId(idCategory).stream()
                .map(e -> modelMapper.map(e, ProductDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/station/{station}")
    public ResponseEntity<List<ProductDTO>> findByStation(@PathVariable("station") KitchenStation station) {
        List<ProductDTO> list = service.findByStation(station).stream()
                .map(e -> modelMapper.map(e, ProductDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Product obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, ProductDTO.class));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> save(@Valid @RequestBody ProductDTO dto) throws Exception {
        Product obj = service.save(modelMapper.map(dto, Product.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdProduct()).toUri();
        return ResponseEntity.created(location).body(modelMapper.map(obj, ProductDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@Valid @RequestBody ProductDTO dto, @PathVariable("id") Integer id) throws Exception {
        Product obj = service.update(modelMapper.map(dto, Product.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, ProductDTO.class));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ProductDTO> updatePrice(@PathVariable("id") Integer id, @RequestBody Map<String, BigDecimal> body) throws Exception {
        BigDecimal newPrice = body.get("price");
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        Product obj = service.updatePrice(id, newPrice, currentUser);
        return ResponseEntity.ok(modelMapper.map(obj, ProductDTO.class));
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<PriceHistoryDTO>> getPriceHistory(@PathVariable("id") Integer id) {
        List<PriceHistoryDTO> list = service.getPriceHistories(id).stream()
                .map(h -> new PriceHistoryDTO(
                        h.getIdPriceHistory(),
                        h.getProduct().getIdProduct(),
                        h.getProduct().getName(),
                        h.getPreviousPrice(),
                        h.getNewPrice(),
                        h.getChangedAt(),
                        h.getUser() != null ? h.getUser().getUsername() : "SISTEMA"
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Product>> listPageable(Pageable pageable) {
        return ResponseEntity.ok(service.listPage(pageable));
    }
}

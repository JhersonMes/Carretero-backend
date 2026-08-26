package com.carretero.controller;

import com.carretero.dto.FlavorDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.Flavor;
import com.carretero.repository.ICategoryRepository;
import com.carretero.repository.IFlavorRepository;
import com.carretero.repository.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/flavors")
@RequiredArgsConstructor
public class FlavorController {

    private final IFlavorRepository repo;
    private final ICategoryRepository categoryRepo;
    private final IProductRepository productRepo;

    /**
     * Opciones que debe ofrecer la pantalla de venta para un producto: las propias
     * del producto si tiene, y si no las compartidas por su categoria.
     */
    @GetMapping("/product/{idProduct}")
    public ResponseEntity<List<FlavorDTO>> findForProduct(@PathVariable("idProduct") Integer idProduct) throws Exception {
        List<Flavor> own = repo.findByProductIdProductAndActiveTrueOrderByOrderIndexAsc(idProduct);
        if (!own.isEmpty()) {
            return ResponseEntity.ok(own.stream().map(this::mapToDTO).toList());
        }

        var product = productRepo.findById(idProduct)
                .orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + idProduct));
        Integer idCategory = product.getCategory() != null ? product.getCategory().getIdCategory() : null;
        if (idCategory == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(
                repo.findByCategoryIdCategoryAndActiveTrueOrderByOrderIndexAsc(idCategory).stream()
                        .map(this::mapToDTO)
                        .toList());
    }

    @GetMapping("/category/{idCategory}")
    public ResponseEntity<List<FlavorDTO>> findByCategory(@PathVariable("idCategory") Integer idCategory) {
        return ResponseEntity.ok(
                repo.findByCategoryIdCategoryAndProductIsNullOrderByOrderIndexAsc(idCategory).stream()
                        .map(this::mapToDTO)
                        .toList());
    }

    /** Todas las del producto, incluidas inactivas, para administrarlas. */
    @GetMapping("/product/{idProduct}/all")
    public ResponseEntity<List<FlavorDTO>> findAllOfProduct(@PathVariable("idProduct") Integer idProduct) {
        return ResponseEntity.ok(
                repo.findByProductIdProductOrderByOrderIndexAsc(idProduct).stream()
                        .map(this::mapToDTO)
                        .toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FlavorDTO> create(@RequestBody FlavorDTO dto) throws Exception {
        Flavor saved = repo.save(applyToEntity(new Flavor(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FlavorDTO> update(@PathVariable("id") Integer id, @RequestBody FlavorDTO dto) throws Exception {
        Flavor existing = repo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Opcion no encontrada: " + id));
        return ResponseEntity.ok(mapToDTO(repo.save(applyToEntity(existing, dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Flavor applyToEntity(Flavor f, FlavorDTO dto) throws Exception {
        f.setName(dto.getName());
        f.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        f.setActive(dto.isActive());
        f.setPriceDelta(dto.getPriceDelta() != null ? dto.getPriceDelta() : BigDecimal.ZERO);

        // Una opcion pertenece a un producto o a una categoria, nunca a los dos.
        if (dto.getIdProduct() != null) {
            f.setProduct(productRepo.findById(dto.getIdProduct())
                    .orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + dto.getIdProduct())));
            f.setCategory(null);
        } else if (dto.getIdCategory() != null) {
            f.setCategory(categoryRepo.findById(dto.getIdCategory())
                    .orElseThrow(() -> new ModelNotFoundException("Categoria no encontrada: " + dto.getIdCategory())));
            f.setProduct(null);
        } else {
            throw new IllegalArgumentException("La opcion debe pertenecer a un producto o a una categoria.");
        }
        return f;
    }

    private FlavorDTO mapToDTO(Flavor f) {
        return new FlavorDTO(
                f.getIdFlavor(),
                f.getName(),
                f.getCategory() != null ? f.getCategory().getIdCategory() : null,
                f.getProduct() != null ? f.getProduct().getIdProduct() : null,
                f.getPriceDelta(),
                f.getOrderIndex(),
                f.isActive()
        );
    }
}

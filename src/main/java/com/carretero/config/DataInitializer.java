package com.carretero.config;

import com.carretero.model.*;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.TableStatus;
import com.carretero.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IRoleRepository roleRepo;
    private final IUserRepository userRepo;
    private final IPaymentMethodRepository paymentMethodRepo;
    private final ICategoryRepository categoryRepo;
    private final IProductRepository productRepo;
    private final IDiningTableRepository tableRepo;
    private final IBusinessConfigRepository configRepo;
    private final IFlavorRepository flavorRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initAreaAndStationUsers();
        initPaymentMethods();
        initCarteraCarretero();
        initBebidas();
        initDiningTables();
        backfillTableOrder();
        initConfig();
    }

    private void initRoles() {
        createRoleIfNotFound("ADMIN", "Administrador total del sistema");
        createRoleIfNotFound("CAJERO", "Cajero / Responsable de turnos de caja y cobros");
        createRoleIfNotFound("MESERO", "Mesero / Toma de pedidos en salón y delivery");
        createRoleIfNotFound("COCINA", "Estación de cocina / Comandas de platos y broasters");
        createRoleIfNotFound("PARRILLA", "Estación de parrilla / Hamburguesas a la brasa");
        createRoleIfNotFound("BEBIDAS", "Estación de bar / Bebidas y tragos");
    }

    private void createRoleIfNotFound(String name, String desc) {
        if (roleRepo.findOneByName(name) == null) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(desc);
            roleRepo.save(role);
            log.info("Rol inicial creado: {}", name);
        }
    }

    private void initAdminUser() {
        if (userRepo.findOneByUsername("admin") == null) {
            Role adminRole = roleRepo.findOneByName("ADMIN");
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@elcarretero.pe");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrador El Carretero");
            admin.setPhone("999888777");
            admin.setEnabled(true);
            admin.setRole(adminRole);
            userRepo.save(admin);
            log.info("Usuario administrador inicial creado: admin / admin123");
        }
    }

    /**
     * Crea un unico usuario por cada area de produccion (cocina, parrilla, bebidas)
     * y las estaciones fisicas de caja y de mesero (4 de cada una), para que el
     * personal de salon y de cocina pueda iniciar sesion de forma independiente.
     */
    private void initAreaAndStationUsers() {
        for (int i = 1; i <= 4; i++) {
            createStationUserIfNotFound("caja" + i, "caja" + i + "@carretero.com", "Caja " + i, "CAJERO");
        }
        for (int i = 1; i <= 4; i++) {
            createStationUserIfNotFound("mesero" + i, "mesero" + i + "@carretero.com", "Mesero " + i, "MESERO");
        }
        createStationUserIfNotFound("cocina", "cocina@carretero.com", "Area de Cocina", "COCINA");
        createStationUserIfNotFound("parrilla", "parrilla@carretero.com", "Area de Parrilla", "PARRILLA");
        createStationUserIfNotFound("bebidas", "bebidas@carretero.com", "Area de Bebidas", "BEBIDAS");
    }

    private void createStationUserIfNotFound(String username, String email, String fullName, String roleName) {
        if (userRepo.findOneByUsername(username) != null) {
            return;
        }
        Role role = roleRepo.findOneByName(roleName);
        if (role == null) {
            log.warn("No se pudo crear el usuario {}: el rol {} no existe.", username, roleName);
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(username + "123"));
        user.setFullName(fullName);
        user.setEnabled(true);
        user.setRole(role);
        userRepo.save(user);
        log.info("Usuario de estacion creado: {} / {}123 ({})", username, username, roleName);
    }

    private void initPaymentMethods() {
        createPaymentMethodIfNotFound("EFECTIVO", "EFE");
        createPaymentMethodIfNotFound("YAPE", "YAP");
        createPaymentMethodIfNotFound("PLIN", "PLI");
        createPaymentMethodIfNotFound("TARJETA", "TAR");
        createPaymentMethodIfNotFound("TRANSFERENCIA", "TRF");
    }

    private void createPaymentMethodIfNotFound(String name, String code) {
        if (paymentMethodRepo.findAll().stream().noneMatch(m -> m.getName().equalsIgnoreCase(name))) {
            PaymentMethod method = new PaymentMethod();
            method.setName(name);
            method.setCode(code);
            method.setActive(true);
            paymentMethodRepo.save(method);
            log.info("Método de pago inicial creado: {}", name);
        }
    }

    /** Nombres del catalogo demo original, reemplazado por la carta real de El Carretero. */
    private static final List<String> LEGACY_CATEGORIES = List.of(
            "Hamburguesas a la Parrilla",
            "Alitas & Broaster",
            "Bebidas & Refrescos",
            "Guarniciones & Extras");

    private static final String CAT_ADICIONALES = "ADICIONALES";
    private static final String CAT_ALITAS_CERDO = "ALITAS DE CERDO";
    private static final String CAT_ALITAS_POLLO = "ALITAS DE POLLO";

    /**
     * Carga la carta real de El Carretero y retira el catalogo demo.
     *
     * Las categorias y productos antiguos se desactivan en vez de borrarse: ya tienen
     * ventas asociadas y eliminarlos romperia el historial de pedidos y comprobantes.
     * Desactivados desaparecen de las pantallas de venta pero los reportes siguen cuadrando.
     *
     * Los precios se siembran en 0.00 porque la carta no los traia; se cargan desde
     * la pantalla de Productos.
     */
    private void initCarteraCarretero() {
        if (categoryRepo.findByName(CAT_ALITAS_POLLO).isPresent()) {
            return; // ya sembrada
        }

        retireLegacyCatalog();

        Category adicionales = categoryRepo.save(newCategory(
                CAT_ADICIONALES, "Adicionales, combos, promociones y otros productos", KitchenStation.PARRILLA, 1));
        Category alitasCerdo = categoryRepo.save(newCategory(
                CAT_ALITAS_CERDO, "Alitas de cerdo por unidades, con eleccion de sabor", KitchenStation.PARRILLA, 2));
        Category alitasPollo = categoryRepo.save(newCategory(
                CAT_ALITAS_POLLO, "Alitas de pollo por unidades, con eleccion de sabor", KitchenStation.COCINA, 3));

        // --- ADICIONALES: productos sueltos, sin eleccion de sabor ---
        List<String> adicionalesNames = List.of(
                "2 Hamburguesas Clásicas + Gaseosa",
                "2x1 Hamburguesas",
                "Adicional De Carne",
                "Adicional De Cerdo",
                "Adicional De Champiñones",
                "Adicional De Chorizo",
                "Adicional De Cremas",
                "Adicional De Jamón",
                "Adicional De Piña",
                "Adicional De Plátanos",
                "Adicional De Pollo",
                "Adicional De Queso",
                "Aros De Cebolla/Caramelo",
                "Chorizo Artesanal",
                "Combo para 3",
                "Combo para dos",
                "Encurtidos Porción",
                "Festival Burger Irrepetible",
                "Huevo",
                "Mermelada De Tocino",
                "Porción De Aros De Cebolla (Con Salsa)",
                "Porción De Papas",
                "Porción De Tocino",
                "Promo Dia del Padre",
                "Promo Irrepetible",
                "Salchicha En Porción",
                "Taper Para Llevar",
                "Tus Hamburguesas Favoritas",
                "Vasos Descartables");
        for (String name : adicionalesNames) {
            productRepo.save(newProduct(name, adicionales, KitchenStation.PARRILLA, false));
        }
        // Envases y descartables no pasan por cocina.
        deactivateKitchenFor(List.of("Taper Para Llevar", "Vasos Descartables"));

        // --- ALITAS DE CERDO: piden sabor ---
        for (String name : List.of("Alitas De Cerdo X 3 Unds", "Alitas De Cerdo X 4 Unds", "Alitas De Cerdo X 6 Unds")) {
            productRepo.save(newProduct(name, alitasCerdo, KitchenStation.PARRILLA, true));
        }
        saveFlavors(alitasCerdo, List.of("Clasicas", "BBQ", "BBQ Picante", "Maracuyá"));

        // --- ALITAS DE POLLO: las porciones por unidades piden sabor; el resto no ---
        for (String name : List.of("Alitas De Pollo X 4", "Alitas De Pollo X 6", "Alitas De Pollo X 8", "Alitas De Pollo X 10")) {
            productRepo.save(newProduct(name, alitasPollo, KitchenStation.COCINA, true));
        }
        for (String name : List.of("ALITAS BROASTHER", "MUSLITOS Broasther", "Chicharrón Pollero", "Criollas / con salsa anticuchera")) {
            productRepo.save(newProduct(name, alitasPollo, KitchenStation.COCINA, false));
        }
        saveFlavors(alitasPollo, List.of(
                "Clasicas", "BBQ", "BBQ Picante", "BBQ Chocolate", "BBQ Durazno",
                "Maracuyá", "Maracumango (Maracuyá + Mango)", "Jalapeño", "Acevichadas"));

        log.info("Carta de El Carretero cargada: {} categorias nuevas. Los precios estan en 0.00 y deben cargarse desde la pantalla de Productos.", 3);
    }

    private static final String CAT_BEBIDAS = "BEBIDAS";

    /**
     * Carta de bebidas. A diferencia de las alitas, aqui cada bebida tiene su propia
     * lista de opciones (la limonada de vaso ofrece cedron y la de litro no), asi que
     * las opciones se cuelgan del producto y no de la categoria.
     */
    private void initBebidas() {
        if (categoryRepo.findByName(CAT_BEBIDAS).isPresent()) {
            return;
        }

        Category bebidas = categoryRepo.save(newCategory(
                CAT_BEBIDAS, "Gaseosas, jugos, infusiones, cafe y cervezas", KitchenStation.BEBIDAS, 4));

        seedDrink(bebidas, "Agua Mineral", List.of("Con gas", "Sin gas"));

        // El cafe cambia de precio segun la preparacion: base S/ 7.00 (americano)
        // y S/ 1.00 mas para las preparaciones con leche.
        Product cafe = productRepo.save(newProduct("Café", bebidas, KitchenStation.BEBIDAS, true));
        cafe.setPrice(new BigDecimal("7.00"));
        cafe.setDescription("Precio base americano; las preparaciones con leche suman S/ 1.00");
        productRepo.save(cafe);
        saveProductFlavors(cafe, List.of(
                new String[] { "Americano", "0.00" },
                new String[] { "Latte", "1.00" },
                new String[] { "Macchiato", "1.00" },
                new String[] { "Capuccino", "1.00" }));

        seedDrink(bebidas, "Chicha Morada", List.of("1 Litro", "500 Ml"));
        seedDrink(bebidas, "Cerveza", List.of("Artesanal", "Nacional"));
        seedDrink(bebidas, "Emoliente Frutado",
                List.of("Maracuyá", "Piña", "Menta", "Hierba Luisa", "Hierba Buena", "Eucalipto y Kión"));
        seedDrink(bebidas, "Gaseosas",
                List.of("Coca Cola", "Inca Cola", "Sprite", "Fanta Amarilla", "Fanta Roja", "Fanta Naranja"));
        seedDrink(bebidas, "Limonada 1 Litro",
                List.of("Natural", "Menta", "Hierba Buena", "Hierba Luisa", "Eucalipto con Kión"));
        seedDrink(bebidas, "Limonada Vaso",
                List.of("Natural", "Menta", "Hierba Buena", "Hierba Luisa", "Eucalipto con Kión", "Cedrón"));
        seedDrink(bebidas, "Limonada Frozen Vaso", List.of("Natural", "Maracuyá"));
        seedDrink(bebidas, "Maracuyá", List.of("1 Litro", "500 Ml"));

        // Sin variantes: entran directo al pedido.
        productRepo.save(newProduct("Emoliente Natural", bebidas, KitchenStation.BEBIDAS, false));
        productRepo.save(newProduct("Guaraná 500 Ml", bebidas, KitchenStation.BEBIDAS, false));

        log.info("Carta de BEBIDAS cargada. Los precios estan en 0.00 salvo el cafe; cargalos desde la pantalla de Carta.");
    }

    /** Crea una bebida con sus opciones propias, todas sin ajuste de precio. */
    private void seedDrink(Category category, String productName, List<String> optionNames) {
        Product product = productRepo.save(newProduct(productName, category, KitchenStation.BEBIDAS, true));
        saveProductFlavors(product, optionNames.stream()
                .map(n -> new String[] { n, "0.00" })
                .toList());
    }

    private void saveProductFlavors(Product product, List<String[]> nameAndDelta) {
        int index = 1;
        for (String[] pair : nameAndDelta) {
            Flavor f = new Flavor();
            f.setName(pair[0]);
            f.setProduct(product);
            f.setCategory(null);
            f.setPriceDelta(new BigDecimal(pair[1]));
            f.setOrderIndex(index++);
            f.setActive(true);
            flavorRepo.save(f);
        }
    }

    /** Desactiva el catalogo demo conservando su historial de ventas. */
    private void retireLegacyCatalog() {
        for (String legacyName : LEGACY_CATEGORIES) {
            categoryRepo.findByName(legacyName).ifPresent(category -> {
                productRepo.findByCategoryIdCategory(category.getIdCategory()).forEach(product -> {
                    product.setActive(false);
                    productRepo.save(product);
                });
                category.setActive(false);
                categoryRepo.save(category);
                log.info("Categoria demo desactivada (historial intacto): {}", legacyName);
            });
        }
    }

    private Category newCategory(String name, String description, KitchenStation station, int orderIndex) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        c.setStation(station);
        c.setOrderIndex(orderIndex);
        c.setActive(true);
        return c;
    }

    private Product newProduct(String name, Category category, KitchenStation station, boolean requiresFlavor) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.ZERO);
        p.setCategory(category);
        p.setStation(station);
        p.setRequiresKitchen(true);
        p.setRequiresFlavor(requiresFlavor);
        p.setActive(true);
        p.setManageStock(false);
        p.setStock(BigDecimal.ZERO);
        return p;
    }

    private void deactivateKitchenFor(List<String> productNames) {
        for (String name : productNames) {
            productRepo.findByName(name).ifPresent(p -> {
                p.setRequiresKitchen(false);
                p.setStation(null);
                productRepo.save(p);
            });
        }
    }

    private void saveFlavors(Category category, List<String> names) {
        int index = 1;
        for (String name : names) {
            Flavor f = new Flavor();
            f.setName(name);
            f.setCategory(category);
            f.setOrderIndex(index++);
            f.setActive(true);
            flavorRepo.save(f);
        }
    }

    private void initDiningTables() {
        if (tableRepo.count() == 0) {
            List<String> tableNames = List.of("Mesa 1", "Mesa 2", "Mesa 3", "Mesa 4", "Mesa 5", "Mesa 6", "Barra 1", "Barra 2");
            for (String name : tableNames) {
                DiningTable t = new DiningTable();
                t.setName(name);
                t.setCapacity(name.contains("Barra") ? 2 : 4);
                t.setStatus(TableStatus.LIBRE);
                t.setActive(true);
                tableRepo.save(t);
            }
            log.info("Mesas de salón iniciales registradas.");
        }
    }

    /**
     * Asigna una posicion en el plano a las mesas que aun no la tienen.
     *
     * Las mesas creadas antes de existir la columna order_index quedan en null y
     * caerian todas al final del salon en el mismo monton. Se les da un orden
     * inicial por id (que es el orden en que se registraron) y a partir de ahi el
     * administrador las reacomoda arrastrandolas. Solo toca las que estan en null,
     * asi que nunca pisa un plano ya acomodado a mano.
     */
    private void backfillTableOrder() {
        List<DiningTable> pending = tableRepo.findAllOrdered().stream()
                .filter(t -> t.getOrderIndex() == null)
                .toList();

        if (pending.isEmpty()) {
            return;
        }

        int next = tableRepo.findAllOrdered().stream()
                .map(DiningTable::getOrderIndex)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(0);

        for (DiningTable table : pending) {
            table.setOrderIndex(next++);
            tableRepo.save(table);
        }
        log.info("Se asigno posicion en el plano del salon a {} mesa(s) sin orden previo.", pending.size());
    }

    private void initConfig() {
        if (configRepo.count() == 0) {
            BusinessConfig config = new BusinessConfig();
            // Datos de la ficha RUC de SUNAT: son los que encabezan cada boleta,
            // asi que van tal como estan inscritos y no con un nombre de fantasia.
            config.setBusinessName("EL CARRETERO E.I.R.L.");
            config.setCommercialName("EL CARRETERO");
            config.setRuc("20610046097");
            config.setAddress("Jr. Irene Silva Nro. 183, Urb. Horacio Zevallos - Cajamarca");
            config.setPhone("993793724");
            config.setEmail("contacto@elcarretero.pe");
            config.setBoletaSeries("B001");
            config.setFacturaSeries("F001");
            config.setNotaVentaSeries("NV01");
            // PIN que autoriza anular o corregir una venta. Se guarda cifrado y
            // solo se puede reemplazar, nunca consultar.
            config.setAdminPin(passwordEncoder.encode("5555"));
            configRepo.save(config);
            log.warn("PIN de anulacion inicial: 5555. Cambialo desde PUT /business-config/admin-pin.");
            log.info("Configuración general inicial de El Carretero registrada.");
        }
    }
}

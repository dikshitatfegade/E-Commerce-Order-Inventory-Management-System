package com.prem.oms.config;

import com.prem.oms.entity.*;
import com.prem.oms.repository.CategoryRepository;
import com.prem.oms.repository.InventoryRepository;
import com.prem.oms.repository.ProductRepository;
import com.prem.oms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Seeds a default admin, a demo customer and sample catalog data on first start. */
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository users, CategoryRepository categories, ProductRepository products,
                           InventoryRepository inventory, PasswordEncoder encoder) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.inventory = inventory;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() == 0) {
            createUser("Admin", "admin@oms.com", "Admin@123", Role.ADMIN);
            createUser("Demo Customer", "customer@oms.com", "Customer@123", Role.CUSTOMER);
        }
        if (categories.count() == 0) {
            Category electronics = category("Electronics");
            Category books = category("Books");
            Category home = category("Home & Kitchen");
            product("Wireless Mouse", "Ergonomic 2.4GHz wireless mouse", "799.00", "ELEC-001", electronics, 50);
            product("Mechanical Keyboard", "Blue switch mechanical keyboard", "2499.00", "ELEC-002", electronics, 25);
            product("USB-C Hub", "7-in-1 USB-C hub with HDMI", "1599.00", "ELEC-003", electronics, 30);
            product("Clean Code", "A Handbook of Agile Software Craftsmanship", "899.00", "BOOK-001", books, 40);
            product("Effective Java", "Best practices for the Java platform", "1099.00", "BOOK-002", books, 20);
            product("Steel Water Bottle", "1L insulated stainless steel bottle", "649.00", "HOME-001", home, 60);
        }
    }

    private void createUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        users.save(u);
    }

    private Category category(String name) {
        Category c = new Category();
        c.setName(name);
        return categories.save(c);
    }

    private void product(String name, String desc, String price, String sku, Category cat, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(new BigDecimal(price));
        p.setSku(sku);
        p.setCategory(cat);
        p = products.save(p);
        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantity(stock);
        inventory.save(inv);
    }
}

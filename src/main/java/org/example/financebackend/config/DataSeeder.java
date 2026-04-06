package org.example.financebackend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.financebackend.model.*;
import org.example.financebackend.repository.PermissionRepository;
import org.example.financebackend.repository.RecordRepository;
import org.example.financebackend.repository.RoleRepository;
import org.example.financebackend.repository.UserRepository;
import org.example.financebackend.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository       userRepository;
    private final RecordRepository     recordRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository       roleRepository;
    private final UserRoleRepository   userRoleRepository;
    private final PasswordEncoder      passwordEncoder;

    @PostConstruct
    @Transactional
    public void seed() {
        seedPermissions();
        seedRoles();
        seedUsersAndRecords();
        seedUserRoleAssignments();
    }

    // â”€â”€ 1. Permissions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void seedPermissions() {
        if (permissionRepository.count() > 0) {
            log.info("Permissions already seeded â€” skipping");
            return;
        }
        log.info("Seeding permissions...");
        List<Permission> permissions = List.of(
                perm("records:read",      "records",   "read",    "View financial records"),
                perm("records:write",     "records",   "write",   "Create and update financial records"),
                perm("records:delete",    "records",   "delete",  "Soft-delete financial records"),
                perm("records:history",   "records",   "history", "View Envers audit history for records"),
                perm("dashboard:view",    "dashboard", "view",    "View dashboard summaries and trends"),
                perm("dashboard:export",  "dashboard", "export",  "Export dashboard data"),
                perm("users:read",        "users",     "read",    "List and view users"),
                perm("users:manage",      "users",     "manage",  "Create, edit roles and status of users")
        );
        permissionRepository.saveAll(permissions);
        log.info("Seeded {} permissions", permissions.size());
    }

    // â”€â”€ 2. Roles â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void seedRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles already seeded â€” skipping");
            return;
        }
        log.info("Seeding roles...");

        // Load all permissions into a lookup map by name
        Map<String, Permission> perms = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getName, Function.identity()));

        Role viewer = roleRepository.save(Role.builder()
                .name("viewer")
                .description("Read-only access to records and dashboard")
                .permissions(Set.of(
                        perms.get("records:read"),
                        perms.get("dashboard:view")
                ))
                .build());

        Role analyst = roleRepository.save(Role.builder()
                .name("analyst")
                .description("Extended read access including audit history and export")
                .permissions(Set.of(
                        perms.get("records:read"),
                        perms.get("records:history"),
                        perms.get("dashboard:view"),
                        perms.get("dashboard:export")
                ))
                .build());

        roleRepository.save(Role.builder()
                .name("admin")
                .description("Full access to all resources")
                .permissions(Set.copyOf(perms.values()))   // all 8 permissions
                .build());

        log.info("Seeded 3 roles (viewer, analyst, admin)");
    }

    //3. Users + records 
    private void seedUsersAndRecords() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded â€” skipping");
            return;
        }
        log.info("Seeding users and financial records...");

        Map<String, Role> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, Function.identity()));

        // Create users first (without roles, so we have their IDs for UserRole.assignedBy)
        User admin = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@finance.local")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .status(UserStatus.ACTIVE)
                .build());

        User analyst = userRepository.save(User.builder()
                .name("Analyst User")
                .email("analyst@finance.local")
                .passwordHash(passwordEncoder.encode("Analyst1234!"))
                .status(UserStatus.ACTIVE)
                .build());

        User viewer = userRepository.save(User.builder()
                .name("Viewer User")
                .email("viewer@finance.local")
                .passwordHash(passwordEncoder.encode("Viewer1234!"))
                .status(UserStatus.ACTIVE)
                .build());

        // Assign roles via UserRole join table (admin is the "assigner" for seeded data)
        assignRole(admin,   roles.get("admin"),   admin);
        assignRole(analyst, roles.get("analyst"), admin);
        assignRole(viewer,  roles.get("viewer"),  admin);

        // Persist role assignments
        userRepository.save(admin);
        userRepository.save(analyst);
        userRepository.save(viewer);

        // â”€â”€ 30 financial records â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        LocalDate today = LocalDate.now();
        List<FinancialRecord> records = List.of(
                // Admin INCOME (5)
                record(admin,   new BigDecimal("5200.00"), RecordType.INCOME,   "Salary",              today.minusDays(1),  "Monthly salary"),
                record(admin,   new BigDecimal("1500.00"), RecordType.INCOME,   "Freelance",           today.minusDays(5),  "Web project payment"),
                record(admin,   new BigDecimal("320.00"),  RecordType.INCOME,   "Dividends",           today.minusDays(10), "Q1 dividends"),
                record(admin,   new BigDecimal("85.00"),   RecordType.INCOME,   "Interest",            today.minusDays(15), "Savings account interest"),
                record(admin,   new BigDecimal("900.00"),  RecordType.INCOME,   "Rental Income",       today.minusDays(20), "Apartment rental"),
                // Admin EXPENSE (10)
                record(admin,   new BigDecimal("1200.00"), RecordType.EXPENSE,  "Rent",                today.minusDays(2),  "Monthly rent"),
                record(admin,   new BigDecimal("350.00"),  RecordType.EXPENSE,  "Groceries",           today.minusDays(3),  "Supermarket weekly shop"),
                record(admin,   new BigDecimal("120.00"),  RecordType.EXPENSE,  "Utilities",           today.minusDays(7),  "Electricity bill"),
                record(admin,   new BigDecimal("75.00"),   RecordType.EXPENSE,  "Transport",           today.minusDays(8),  "Monthly bus pass"),
                record(admin,   new BigDecimal("90.00"),   RecordType.EXPENSE,  "Entertainment",       today.minusDays(12), "Streaming + cinema"),
                record(admin,   new BigDecimal("200.00"),  RecordType.EXPENSE,  "Healthcare",          today.minusDays(14), "Doctor visit"),
                record(admin,   new BigDecimal("150.00"),  RecordType.EXPENSE,  "Insurance",           today.minusDays(18), "Car insurance"),
                record(admin,   new BigDecimal("180.00"),  RecordType.EXPENSE,  "Dining",              today.minusDays(22), "Restaurant meals"),
                record(admin,   new BigDecimal("45.00"),   RecordType.EXPENSE,  "Subscriptions",       today.minusDays(25), "Software subscriptions"),
                record(admin,   new BigDecimal("60.00"),   RecordType.EXPENSE,  "Office Supplies",     today.minusDays(28), "Printer paper and pens"),
                // Admin TRANSFER (2)
                record(admin,   new BigDecimal("2000.00"), RecordType.TRANSFER, "Savings Transfer",    today.minusDays(4),  "Monthly savings"),
                record(admin,   new BigDecimal("500.00"),  RecordType.TRANSFER, "Investment Transfer", today.minusDays(13), "Index fund purchase"),
                // Analyst INCOME (3)
                record(analyst, new BigDecimal("4000.00"), RecordType.INCOME,   "Salary",              today.minusDays(2),  "Monthly salary"),
                record(analyst, new BigDecimal("600.00"),  RecordType.INCOME,   "Freelance",           today.minusDays(9),  "Logo design project"),
                record(analyst, new BigDecimal("250.00"),  RecordType.INCOME,   "Dividends",           today.minusDays(16), "ETF dividends"),
                // Analyst EXPENSE (7)
                record(analyst, new BigDecimal("1000.00"), RecordType.EXPENSE,  "Rent",                today.minusDays(3),  "Shared apartment"),
                record(analyst, new BigDecimal("220.00"),  RecordType.EXPENSE,  "Groceries",           today.minusDays(6),  "Weekly groceries"),
                record(analyst, new BigDecimal("80.00"),   RecordType.EXPENSE,  "Utilities",           today.minusDays(11), "Internet + water"),
                record(analyst, new BigDecimal("50.00"),   RecordType.EXPENSE,  "Transport",           today.minusDays(17), "Fuel"),
                record(analyst, new BigDecimal("130.00"),  RecordType.EXPENSE,  "Dining",              today.minusDays(23), "Team lunches"),
                record(analyst, new BigDecimal("420.00"),  RecordType.EXPENSE,  "Healthcare",          today.minusDays(30), "Annual checkup"),
                record(analyst, new BigDecimal("190.00"),  RecordType.EXPENSE,  "Entertainment",       today.minusDays(27), "Games and books"),
                // Analyst TRANSFER (1)
                record(analyst, new BigDecimal("800.00"),  RecordType.TRANSFER, "Savings Transfer",    today.minusDays(7),  "Emergency fund top-up"),
                // Admin extra (2)
                record(admin,   new BigDecimal("750.00"),  RecordType.INCOME,   "Freelance",           today.minusDays(35), "App development"),
                record(admin,   new BigDecimal("310.00"),  RecordType.EXPENSE,  "Office Supplies",     today.minusDays(33), "New keyboard and monitor stand")
        );
        recordRepository.saveAll(records);
        log.info("Seeded 3 users and {} financial records", records.size());
    }


    // ── 4. UserRole assignments (repair / idempotent) ──────────────────────────
    private void seedUserRoleAssignments() {
        if (userRoleRepository.count() > 0) {
            log.info("User-role assignments already present — skipping");
            return;
        }
        log.info("No user-role assignments found — assigning default roles to seeded users...");

        Map<String, Role> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, Function.identity()));

        userRepository.findByEmail("admin@finance.local").ifPresent(admin -> {
            Role adminRole = roles.get("admin");
            if (adminRole != null) { assignRole(admin, adminRole, admin); userRepository.save(admin); }
        });
        userRepository.findByEmail("analyst@finance.local").ifPresent(analyst -> {
            userRepository.findByEmail("admin@finance.local").ifPresent(admin -> {
                Role analystRole = roles.get("analyst");
                if (analystRole != null) { assignRole(analyst, analystRole, admin); userRepository.save(analyst); }
            });
        });
        userRepository.findByEmail("viewer@finance.local").ifPresent(viewer -> {
            userRepository.findByEmail("admin@finance.local").ifPresent(admin -> {
                Role viewerRole = roles.get("viewer");
                if (viewerRole != null) { assignRole(viewer, viewerRole, admin); userRepository.save(viewer); }
            });
        });
        log.info("User-role assignments seeded successfully");
    }
    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void assignRole(User user, Role role, User assignedBy) {
        UserRoleId pk = new UserRoleId(user.getId(), role.getId());
        UserRole ur = UserRole.builder()
                .id(pk)
                .user(user)
                .role(role)
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .build();
        user.getUserRoles().add(ur);
    }

    private Permission perm(String name, String resource, String action, String description) {
        return Permission.builder()
                .name(name)
                .resource(resource)
                .action(action)
                .description(description)
                .build();
    }

    private FinancialRecord record(User user, BigDecimal amount, RecordType type,
                                   String category, LocalDate date, String notes) {
        return FinancialRecord.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .category(category)
                .date(date)
                .notes(notes)
                .build();
    }
}

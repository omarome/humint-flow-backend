package com.example.querybuilderapi.config;

import com.example.querybuilderapi.model.Activity;
import com.example.querybuilderapi.model.ActivityType;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.model.Notification;
import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.NotificationRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import com.example.querybuilderapi.repository.UserRepository;
import com.example.querybuilderapi.repository.VariableRepository;
import com.example.querybuilderapi.model.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds initial data for the hobby project:
 * - A default admin user based on environment variables
 * - Demo CRM data (Organizations, Contacts, Opportunities)
 * - Demo Activity timeline data
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData(AuthAccountRepository authRepository,
                                      OrganizationRepository orgRepository,
                                      ContactRepository contactRepository,
                                      OpportunityRepository oppRepository,
                                      ActivityRepository activityRepository,
                                      UserRepository userRepository,
                                      VariableRepository variableRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Admin
            AuthAccount admin = authRepository.findByEmail(adminEmail).orElseGet(() -> {
                log.info("Seeding default admin user: {}", adminEmail);
                AuthAccount newAdmin = new AuthAccount();
                newAdmin.setEmail(adminEmail);
                newAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
                newAdmin.setDisplayName("HumintFlow Admin");
                newAdmin.setRole(AuthAccount.Role.ADMIN);
                newAdmin.setOauthProvider(AuthAccount.OAuthProvider.LOCAL);
                return authRepository.save(newAdmin);
            });

            // Backfill existing contacts missing phone numbers
            contactRepository.findAll().forEach(contact -> {
                if (contact.getPhone() == null || contact.getPhone().isEmpty()) {
                    if ("Hank".equals(contact.getFirstName())) {
                        contact.setPhone("+1 (555) 019-8372");
                    } else if ("Alice".equals(contact.getFirstName())) {
                        contact.setPhone("+44 20 7946 0958");
                    } else {
                        contact.setPhone("+1 (555) 123-4567");
                    }
                    contactRepository.save(contact);
                }
            });

            // Seed Variables (legacy users + CRM entities)
            if (variableRepository.count() == 0) {
                log.info("Seeding query builder variables...");
                variableRepository.saveAll(List.of(
                    // ── Legacy: users table (entityType=null for backward compat) ──
                    new Variable(null, "fullName",   "Full Name",      8,  "STRING",  null),
                    new Variable(null, "email",      "Email",          4,  "EMAIL",   null),
                    new Variable(null, "position",   "Position",       28, "STRING",  null),
                    new Variable(null, "department", "Department",     32, "STRING",  null),
                    new Variable(null, "status",     "Account Status", 24, "STRING",  null),
                    new Variable(null, "isOnline",   "Online Status",  12, "BOOL",    null),

                    // ── CONTACT fields ───────────────────────────────────────────
                    new Variable(null, "firstName",      "First Name",      1,  "STRING",  "CONTACT"),
                    new Variable(null, "lastName",       "Last Name",       2,  "STRING",  "CONTACT"),
                    new Variable(null, "email",          "Email",           3,  "EMAIL",   "CONTACT"),
                    new Variable(null, "phone",          "Phone",           4,  "STRING",  "CONTACT"),
                    new Variable(null, "jobTitle",       "Job Title",       5,  "STRING",  "CONTACT"),
                    new Variable(null, "department",     "Department",      6,  "STRING",  "CONTACT"),
                    new Variable(null, "leadSource",     "Lead Source",     7,  "STRING",  "CONTACT"),
                    new Variable(null, "leadScore",      "Lead Score",      8,  "NUMBER",  "CONTACT"),
                    new Variable(null, "lifecycleStage", "Lifecycle Stage", 9,  "STRING",  "CONTACT"),

                    // ── ORGANIZATION fields ──────────────────────────────────────
                    new Variable(null, "name",            "Name",           1,  "STRING",  "ORGANIZATION"),
                    new Variable(null, "industry",        "Industry",       2,  "STRING",  "ORGANIZATION"),
                    new Variable(null, "website",         "Website",        3,  "STRING",  "ORGANIZATION"),
                    new Variable(null, "numberOfEmployees","Employees",     4,  "NUMBER",  "ORGANIZATION"),
                    new Variable(null, "annualRevenue",   "Annual Revenue", 5,  "NUMBER",  "ORGANIZATION"),
                    new Variable(null, "country",         "Country",        6,  "STRING",  "ORGANIZATION"),
                    new Variable(null, "city",            "City",           7,  "STRING",  "ORGANIZATION"),

                    // ── OPPORTUNITY fields ───────────────────────────────────────
                    new Variable(null, "name",               "Deal Name",    1,  "STRING",  "OPPORTUNITY"),
                    new Variable(null, "stage",              "Stage",        2,  "STRING",  "OPPORTUNITY"),
                    new Variable(null, "amount",             "Amount ($)",   3,  "NUMBER",  "OPPORTUNITY"),
                    new Variable(null, "probability",        "Probability",  4,  "NUMBER",  "OPPORTUNITY"),
                    new Variable(null, "dealType",           "Deal Type",    5,  "STRING",  "OPPORTUNITY"),
                    new Variable(null, "expectedCloseDate",  "Close Date",   6,  "DATE",    "OPPORTUNITY"),
                    new Variable(null, "organizationName",   "Organization", 7,  "STRING",  "OPPORTUNITY"),

                    // ── ACTIVITY fields ──────────────────────────────────────────
                    new Variable(null, "activityType", "Type",      1,  "STRING",  "ACTIVITY"),
                    new Variable(null, "subject",      "Subject",   2,  "STRING",  "ACTIVITY"),
                    new Variable(null, "taskDueDate",  "Due Date",  3,  "DATE",    "ACTIVITY"),

                    // ── TEAM_MEMBER fields ───────────────────────────────────────
                    new Variable(null, "email",      "Email",      1,  "EMAIL",   "TEAM_MEMBER"),
                    new Variable(null, "displayName","Name",       2,  "STRING",  "TEAM_MEMBER"),
                    new Variable(null, "jobTitle",   "Job Title",  3,  "STRING",  "TEAM_MEMBER"),
                    new Variable(null, "department", "Department", 4,  "STRING",  "TEAM_MEMBER"),
                    new Variable(null, "role",       "Role",       5,  "STRING",  "TEAM_MEMBER")
                ));
            }

            // Seed Team Members (Users)
            if (userRepository.count() == 0) {
                log.info("Seeding team members...");
                userRepository.saveAll(List.of(
                    new User(null, "Sarah", "Connor", "sarah@humintflow.ai", "Active", true, "Senior Sales Manager", "Global Sales"),
                    new User(null, "John", "Doe", "john@humintflow.ai", "Active", true, "Business Development Rep", "Inbound"),
                    new User(null, "Mike", "Ross", "mike@humintflow.ai", "Active", false, "Account Executive", "Mid-Market")
                ));
            }

            // Seed CRM Demo Data
            if (orgRepository.count() == 0) {
                log.info("Seeding CRM demo data...");
                List<User> team = userRepository.findAll();
                User manager = team.isEmpty() ? null : team.get(0);
                User rep = team.size() > 1 ? team.get(1) : manager;

                // Seed AuthAccount team members for CRM demo (if not yet created beyond admin)
                AuthAccount salesRep = authRepository.findByEmail("sarah@humintflow.ai").orElseGet(() -> {
                    AuthAccount a = new AuthAccount("sarah@humintflow.ai", null, "Sarah Connor", null);
                    a.setRole(AuthAccount.Role.SALES_REP);
                    a.setJobTitle("Senior Sales Manager");
                    a.setDepartment("Global Sales");
                    a.setIsActive(true);
                    return authRepository.save(a);
                });
                AuthAccount juniorRep = authRepository.findByEmail("john@humintflow.ai").orElseGet(() -> {
                    AuthAccount a = new AuthAccount("john@humintflow.ai", null, "John Doe", null);
                    a.setRole(AuthAccount.Role.SALES_REP);
                    a.setJobTitle("Business Development Rep");
                    a.setDepartment("Inbound");
                    a.setIsActive(true);
                    return authRepository.save(a);
                });
                
                // --- Organization 1: Globex ---
                Organization globex = new Organization();
                globex.setName("Globex Corporation");
                globex.setIndustry("Manufacturing");
                globex.setWebsite("https://globex.example.com");
                globex.setAnnualRevenue(new BigDecimal("50000000.00"));
                globex.setEmployeeCount(1200);
                globex.setCountry("USA");
                globex.setCreatedBy(admin.getId());
                globex.setUpdatedBy(admin.getId());
                globex.setAssignedTo(manager);
                orgRepository.save(globex);

                Contact hank = new Contact();
                hank.setFirstName("Hank");
                hank.setLastName("Scorpio");
                hank.setEmail("hank@globex.example.com");
                hank.setPhone("+1 (555) 019-8372");
                hank.setJobTitle("CEO");
                hank.setOrganization(globex);
                hank.setAssignedTo(manager);
                hank.setCreatedBy(admin.getId());
                hank.setUpdatedBy(admin.getId());
                contactRepository.save(hank);

                Opportunity globexOpp1 = new Opportunity();
                globexOpp1.setName("Global Expansion Phase 1");
                globexOpp1.setAmount(new BigDecimal("250000.00"));
                globexOpp1.setStage(Opportunity.DealStage.NEGOTIATION.name());
                globexOpp1.setProbability(80);
                globexOpp1.setExpectedCloseDate(LocalDate.now().plusDays(15));
                globexOpp1.setOrganization(globex);
                globexOpp1.setPrimaryContact(hank);
                globexOpp1.setAssignedTo(salesRep);
                globexOpp1.setCreatedBy(admin.getId());
                globexOpp1.setUpdatedBy(admin.getId());
                oppRepository.save(globexOpp1);

                // --- Organization 2: Acme Systems ---
                Organization acme = new Organization();
                acme.setName("Acme Systems");
                acme.setIndustry("Technology");
                acme.setWebsite("https://acme.example.com");
                acme.setAnnualRevenue(new BigDecimal("15000000.00"));
                acme.setEmployeeCount(350);
                acme.setCountry("UK");
                acme.setCreatedBy(admin.getId());
                acme.setUpdatedBy(admin.getId());
                acme.setAssignedTo(rep);
                orgRepository.save(acme);

                Contact alice = new Contact();
                alice.setFirstName("Alice");
                alice.setLastName("Wonderland");
                alice.setEmail("alice@acme.example.com");
                alice.setPhone("+44 20 7946 0958");
                alice.setJobTitle("CTO");
                alice.setOrganization(acme);
                alice.setAssignedTo(rep);
                alice.setCreatedBy(admin.getId());
                alice.setUpdatedBy(admin.getId());
                contactRepository.save(alice);

                Opportunity acmeOpp1 = new Opportunity();
                acmeOpp1.setName("Cloud Migration Project");
                acmeOpp1.setAmount(new BigDecimal("75000.00"));
                acmeOpp1.setStage(Opportunity.DealStage.QUALIFICATION.name());
                acmeOpp1.setProbability(30);
                acmeOpp1.setExpectedCloseDate(LocalDate.now().plusDays(45));
                acmeOpp1.setOrganization(acme);
                acmeOpp1.setPrimaryContact(alice);
                acmeOpp1.setAssignedTo(juniorRep);
                acmeOpp1.setCreatedBy(admin.getId());
                acmeOpp1.setUpdatedBy(admin.getId());
                oppRepository.save(acmeOpp1);

                // ─── Seed Activities ──────────────────────────────────────────
                log.info("Seeding demo activities...");

                // Activities on Globex Organization
                seedActivity(activityRepository, ActivityType.NOTE, EntityType.ORGANIZATION, globex.getId(),
                    "Initial Assessment", "Reviewed Globex's annual report. Strong growth in Q3.", admin.getId(),
                    LocalDateTime.now().minusDays(10));
                seedActivity(activityRepository, ActivityType.CALL, EntityType.ORGANIZATION, globex.getId(),
                    "Introductory Call with COO", "Discussed partnership options. 30 min call.", admin.getId(),
                    LocalDateTime.now().minusDays(7));
                seedActivity(activityRepository, ActivityType.EMAIL, EntityType.ORGANIZATION, globex.getId(),
                    "Proposal Follow-up", "Sent revised pricing proposal to leadership team.", admin.getId(),
                    LocalDateTime.now().minusDays(3));

                // Activities on Hank (Contact)
                seedActivity(activityRepository, ActivityType.MEETING, EntityType.CONTACT, hank.getId(),
                    "Lunch Meeting with Hank", "Met at The Capital Grille. Discussed timelines.", admin.getId(),
                    LocalDateTime.now().minusDays(5));
                seedActivity(activityRepository, ActivityType.NOTE, EntityType.CONTACT, hank.getId(),
                    "Key Decision Maker", "Hank confirmed he has final sign-off authority.", admin.getId(),
                    LocalDateTime.now().minusDays(4));
                seedActivity(activityRepository, ActivityType.TASK, EntityType.CONTACT, hank.getId(),
                    "Send Contract to Hank", "Prepare and email the MSA by Friday.", admin.getId(),
                    LocalDateTime.now().minusDays(1));

                // Activities on Globex Opportunity
                seedActivity(activityRepository, ActivityType.NOTE, EntityType.OPPORTUNITY, globexOpp1.getId(),
                    "Pricing Approved", "Client accepted the $250K budget. Moving to legal review.", admin.getId(),
                    LocalDateTime.now().minusDays(2));
                seedActivity(activityRepository, ActivityType.EMAIL, EntityType.OPPORTUNITY, globexOpp1.getId(),
                    "Legal Review Request", "Sent contract to legal@globex for review.", admin.getId(),
                    LocalDateTime.now().minusDays(1));

                // Activities on Acme Organization
                seedActivity(activityRepository, ActivityType.NOTE, EntityType.ORGANIZATION, acme.getId(),
                    "Company Research", "Acme recently secured Series C funding. Good timing.", admin.getId(),
                    LocalDateTime.now().minusDays(14));
                seedActivity(activityRepository, ActivityType.CALL, EntityType.ORGANIZATION, acme.getId(),
                    "Discovery Call", "15 min intro call with VP of Engineering.", admin.getId(),
                    LocalDateTime.now().minusDays(8));

                // Activities on Alice (Contact)
                seedActivity(activityRepository, ActivityType.EMAIL, EntityType.CONTACT, alice.getId(),
                    "Introduction Email", "Sent intro deck and case studies.", admin.getId(),
                    LocalDateTime.now().minusDays(12));
                seedActivity(activityRepository, ActivityType.MEETING, EntityType.CONTACT, alice.getId(),
                    "Video Call Demo", "45 min product demo via Zoom. Very engaged.", admin.getId(),
                    LocalDateTime.now().minusDays(6));

                // Activities on Acme Opportunity
                seedActivity(activityRepository, ActivityType.NOTE, EntityType.OPPORTUNITY, acmeOpp1.getId(),
                    "Budget Discussion", "Alice mentioned $50K-$100K budget range for cloud migration.", admin.getId(),
                    LocalDateTime.now().minusDays(4));
                seedActivity(activityRepository, ActivityType.TASK, EntityType.OPPORTUNITY, acmeOpp1.getId(),
                    "Prepare Technical Proposal", "Create custom architecture doc for Acme's infrastructure.", admin.getId(),
                    LocalDateTime.now().minusDays(1));

            } else {
                log.info("CRM data already seeded, skipping.");
            }

            // Seed activities for existing data (if activities table is empty but CRM data exists)
            if (activityRepository.count() == 0 && orgRepository.count() > 0) {
                log.info("Seeding activities for existing CRM data...");
                List<Organization> orgs = orgRepository.findAll();
                for (Organization org : orgs) {
                    seedActivity(activityRepository, ActivityType.NOTE, EntityType.ORGANIZATION, org.getId(),
                        "Initial Review", "Reviewed " + org.getName() + "'s profile and market position.", admin.getId(),
                        LocalDateTime.now().minusDays(5));
                }
            }
        };
    }

    @Bean
    public CommandLineRunner initNotifications(NotificationRepository notificationRepository) {
        return args -> {
            if (notificationRepository.count() == 0) {
                log.info("Seeding initial notifications...");
                notificationRepository.saveAll(List.of(
                    buildNotification("New Feature Available",
                        "Check out the new Saved Filters sidebar to quickly access your favorite HumintFlow queries.",
                        "info", false, LocalDateTime.now().minusMinutes(30)),
                    buildNotification("Authentication Required",
                        "Your session expires in 2 hours. Please re-authenticate if you plan to continue working in HumintFlow.",
                        "warning", false, LocalDateTime.now().minusDays(1)),
                    buildNotification("Welcome to HumintFlow",
                        "Explore the advanced query builder to construct complex filters visually.",
                        "info", true, LocalDateTime.now().minusDays(2))
                ));
            } else {
                // Migrate existing notifications if they still use the old name
                notificationRepository.findAll().forEach(n -> {
                    boolean updated = false;
                    if (n.getTitle().contains("MashiFlow") || n.getTitle().contains("Smart Filter Hub")) {
                        n.setTitle(n.getTitle().replace("MashiFlow", "HumintFlow").replace("Smart Filter Hub", "HumintFlow"));
                        updated = true;
                    }
                    if (n.getMessage().contains("MashiFlow") || n.getMessage().contains("Smart Filter Hub")) {
                        n.setMessage(n.getMessage().replace("MashiFlow", "HumintFlow").replace("Smart Filter Hub", "HumintFlow"));
                        updated = true;
                    }
                    if (updated) notificationRepository.save(n);
                });
                log.info("Notifications already seeded, branding check complete.");
            }
        };
    }

    private Notification buildNotification(String title, String message, String type, boolean isRead, LocalDateTime timestamp) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setIsRead(isRead);
        n.setTimestamp(timestamp);
        return n;
    }

    private void seedActivity(ActivityRepository repo, ActivityType activityType, EntityType entityType,
                              java.util.UUID entityId, String subject, String body, Long createdBy,
                              LocalDateTime createdAt) {
        Activity a = new Activity();
        a.setActivityType(activityType);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setSubject(subject);
        a.setBody(body);
        a.setCreatedBy(createdBy);
        a.setUpdatedBy(createdBy);

        // Set type-specific defaults
        if (activityType == ActivityType.CALL) {
            a.setCallDuration(1800); // 30 min default
        }
        if (activityType == ActivityType.TASK) {
            a.setTaskDueDate(LocalDate.now().plusDays(3));
            a.setTaskCompleted(false);
        }

        repo.save(a);
    }
}

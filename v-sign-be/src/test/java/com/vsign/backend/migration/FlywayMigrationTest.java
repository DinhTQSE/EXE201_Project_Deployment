package com.vsign.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesSchemaAndSeedMigrations() {
        Integer usersTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_schema) = 'public' and lower(table_name) = 'users'",
                Integer.class
        );

        Integer rolesCount = jdbcTemplate.queryForObject(
                "select count(*) from reference_roles",
                Integer.class
        );

        Integer dictionaryEntriesCount = jdbcTemplate.queryForObject(
                "select count(*) from dictionary_entries where is_published = true",
                Integer.class
        );

        Integer learningUnitsCount = jdbcTemplate.queryForObject(
                "select count(*) from learning_units where is_published = true",
                Integer.class
        );

        Integer practiceItemsCount = jdbcTemplate.queryForObject(
                "select count(*) from practice_items where is_published = true",
                Integer.class
        );

        Integer assessmentsCount = jdbcTemplate.queryForObject(
                "select count(*) from assessments where is_published = true",
                Integer.class
        );

        Integer lessonQuizzesCount = jdbcTemplate.queryForObject(
                "select count(*) from lesson_quizzes where is_published = true",
                Integer.class
        );

        Integer gamificationProfilesCount = jdbcTemplate.queryForObject(
                "select count(*) from gamification_profiles",
                Integer.class
        );

        Integer subscriptionPlansCount = jdbcTemplate.queryForObject(
                "select count(*) from subscription_plans where active = true",
                Integer.class
        );

        Integer paymentOrdersCount = jdbcTemplate.queryForObject(
                "select count(*) from payment_orders",
                Integer.class
        );

        Integer adminUsersCount = jdbcTemplate.queryForObject(
                "select count(*) from admin_user_accounts",
                Integer.class
        );

        Integer adminReviewQueueCount = jdbcTemplate.queryForObject(
                "select count(*) from admin_review_queue",
                Integer.class
        );

        Integer signatureAttemptLogTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_schema) = 'public' and lower(table_name) = 'signature_attempt_logs'",
                Integer.class
        );

        Integer dictionaryVideoVariantsTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_schema) = 'public' and lower(table_name) = 'dictionary_entry_video_variants'",
                Integer.class
        );

        Integer appliedVersions = jdbcTemplate.queryForObject(
                "select count(distinct version) from flyway_schema_history where success = true and version is not null",
                Integer.class
        );

        Integer testActorAccountsCount = jdbcTemplate.queryForObject(
                "select count(*) from users where email in ('learner.basic@vsign.test', 'learner.premium@vsign.test', 'admin@vsign.test', 'superadmin@vsign.test', 'reviewer@vsign.test', 'inactive@vsign.test')",
                Integer.class
        );

        assertThat(usersTableCount).isEqualTo(1);
        assertThat(rolesCount).isEqualTo(4);
        assertThat(dictionaryEntriesCount).isEqualTo(28);
        assertThat(learningUnitsCount).isEqualTo(13);
        assertThat(practiceItemsCount).isEqualTo(126);
        assertThat(assessmentsCount).isEqualTo(3);
        assertThat(lessonQuizzesCount).isEqualTo(43);
        assertThat(gamificationProfilesCount).isEqualTo(5);
        assertThat(subscriptionPlansCount).isEqualTo(4);
        assertThat(paymentOrdersCount).isGreaterThanOrEqualTo(2);
        assertThat(adminUsersCount).isEqualTo(8);
        assertThat(adminReviewQueueCount).isEqualTo(3);
        assertThat(signatureAttemptLogTableCount).isEqualTo(1);
        assertThat(dictionaryVideoVariantsTableCount).isEqualTo(1);
        assertThat(testActorAccountsCount).isEqualTo(6);
        assertThat(appliedVersions).isEqualTo(19);
    }
}

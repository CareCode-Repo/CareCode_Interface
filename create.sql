
    create table care_facility_bookings (
        child_age integer,
        actual_end_time datetime(6),
        actual_start_time datetime(6),
        cancelled_at datetime(6),
        created_at datetime(6) not null,
        end_time datetime(6) not null,
        facility_id bigint not null,
        id bigint not null auto_increment,
        start_time datetime(6) not null,
        updated_at datetime(6) not null,
        cancellation_reason varchar(255),
        child_name varchar(255) not null,
        notes TEXT,
        parent_name varchar(255) not null,
        parent_phone varchar(255) not null,
        special_requirements TEXT,
        user_id varchar(255) not null,
        booking_type enum ('REGULAR','TEMPORARY','VISIT') not null,
        status enum ('CANCELLED','COMPLETED','CONFIRMED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table notification_preferences (
        email_enabled bit not null,
        in_app_enabled bit not null,
        push_enabled bit not null,
        sms_enabled bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        user_id bigint not null,
        device_token varchar(255),
        email_address varchar(255),
        phone_number varchar(255),
        notification_type enum ('COMMUNITY','HEALTH','POLICY','SYSTEM') not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_care_facilities (
        age_range_max integer,
        age_range_min integer,
        available_spots integer,
        capacity integer,
        current_enrollment integer,
        is_active bit,
        is_public bit,
        latitude float(53),
        longitude float(53),
        rating float(53),
        review_count integer,
        subsidy_available bit,
        teacher_count integer,
        tuition_fee integer,
        view_count integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        accreditation varchar(255),
        additional_fees varchar(255),
        address varchar(255),
        age_range varchar(255),
        city varchar(255),
        curriculum varchar(255),
        description varchar(255),
        district varchar(255),
        email varchar(255),
        facilities varchar(255),
        facility_code varchar(255) not null,
        name varchar(255) not null,
        operating_hours varchar(255),
        phone varchar(255),
        student_teacher_ratio varchar(255),
        website varchar(255),
        facility_type enum ('DAYCARE','KINDERGARTEN','NURSERY','OTHER','PLAYGROUP'),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_chat_messages (
        confidence float(53),
        is_helpful bit,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        message varchar(255) not null,
        response varchar(255) not null,
        session_id varchar(255),
        intent_type enum ('COMPLAINT','EDUCATION_INFO','FACILITY_INFO','GOODBYE','GREETING','HEALTH_INFO','POLICY_INFO','QUESTION','THANKS','UNKNOWN') not null,
        message_type enum ('BOT','USER') not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_chat_sessions (
        message_count integer,
        created_at datetime(6) not null,
        ended_at datetime(6),
        id bigint not null auto_increment,
        last_activity_at datetime(6),
        updated_at datetime(6),
        user_id bigint not null,
        description varchar(255),
        session_id varchar(255) not null,
        title varchar(255) not null,
        status enum ('ACTIVE','ENDED','PAUSED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_child (
        age integer,
        birth_date date,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        gender varchar(255),
        name varchar(255) not null,
        special_needs varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_comment (
        is_active bit,
        like_count integer,
        author_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        parent_comment_id bigint,
        post_id bigint not null,
        updated_at datetime(6),
        author_name varchar(255),
        content TEXT not null,
        status enum ('DELETED','HIDDEN','PUBLISHED'),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_email_verification_token (
        used bit not null,
        expiry_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        token varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_health_record (
        height float(53),
        is_completed bit,
        next_date date,
        pulse_rate integer,
        record_date date not null,
        temperature float(53),
        weight float(53),
        child_id bigint,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        record_type_id bigint,
        updated_at datetime(6),
        user_id bigint not null,
        blood_pressure varchar(255),
        description TEXT,
        diagnosis TEXT,
        doctor_name varchar(255),
        hospital_name varchar(255),
        location varchar(255),
        medication TEXT,
        notes TEXT,
        symptoms TEXT,
        title varchar(255) not null,
        treatment TEXT,
        vaccine_batch varchar(255),
        vaccine_name varchar(255),
        record_type enum ('CHECKUP','DENTAL','EMERGENCY','EYE','GROWTH','ILLNESS','OTHER','VACCINATION') not null,
        status enum ('CANCELLED','COMPLETED','IN_PROGRESS','MISSED','SCHEDULED'),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_health_record_attachments (
        display_order integer not null,
        is_active bit not null,
        created_at datetime(6) not null,
        file_size bigint,
        health_record_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        file_type varchar(50),
        file_name varchar(200) not null,
        description varchar(500),
        file_url varchar(500) not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_health_record_types (
        display_order integer not null,
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        category varchar(50),
        name varchar(100) not null,
        description varchar(500),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_hospital (
        latitude float(53),
        longitude float(53),
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        address varchar(255),
        name varchar(255) not null,
        phone varchar(255),
        type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_hospital_like (
        created_at datetime(6),
        hospital_id bigint not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_hospital_review (
        rating integer not null,
        created_at datetime(6),
        hospital_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        content TEXT,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_notification (
        is_read bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        message TEXT not null,
        title varchar(255) not null,
        notification_type enum ('COMMUNITY','HEALTH','POLICY','SYSTEM') not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_notification_channel (
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        description varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_notification_settings (
        chatbot_notification bit,
        community_notification bit,
        email_notification bit,
        facility_notification bit,
        policy_notification bit,
        push_notification bit,
        quiet_hours_enabled bit,
        quiet_hours_end time(6),
        quiet_hours_start time(6),
        sms_notification bit,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_notification_templates (
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        template_code varchar(50) not null,
        template_type varchar(50),
        title varchar(200) not null,
        description varchar(500),
        content TEXT not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_policies (
        application_end_date date,
        application_start_date date,
        benefit_amount integer,
        is_active bit,
        policy_end_date date,
        policy_start_date date,
        priority integer,
        target_age_max integer,
        target_age_min integer,
        category_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        application_url varchar(255),
        benefit_type varchar(255),
        contact_info varchar(255),
        description TEXT,
        policy_code varchar(255) not null,
        policy_type varchar(255),
        required_documents varchar(255),
        target_region varchar(255),
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_policy_categories (
        display_order integer not null,
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        name varchar(100) not null,
        description varchar(500),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_policy_documents (
        display_order integer not null,
        is_active bit not null,
        created_at datetime(6) not null,
        file_size bigint,
        id bigint not null auto_increment,
        policy_id bigint not null,
        updated_at datetime(6),
        document_type varchar(50),
        file_name varchar(200) not null,
        description varchar(500),
        document_url varchar(500) not null,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_post (
        comment_count integer,
        is_active bit,
        is_anonymous bit,
        like_count integer,
        view_count integer,
        author_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        author_name varchar(255),
        content TEXT,
        title varchar(255) not null,
        category enum ('EVENT','GENERAL','NEWS','NOTICE','QUESTION','REVIEW','SHARE') not null,
        status enum ('DELETED','DRAFT','HIDDEN','PUBLISHED'),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_post_tags (
        post_id bigint not null,
        tag_id bigint not null
    ) engine=InnoDB;

    create table tbl_reviews (
        is_active bit not null,
        is_verified bit not null,
        rating integer not null,
        created_at datetime(6) not null,
        facility_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        content TEXT,
        primary key (id)
    ) engine=InnoDB;

    create table tbl_tag (
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        name varchar(50) not null,
        description varchar(200),
        primary key (id)
    ) engine=InnoDB;

    create table tbl_user (
        birth_date date,
        email_verified bit not null,
        is_active bit not null,
        latitude float(53),
        longitude float(53),
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        last_login_at datetime(6),
        updated_at datetime(6),
        address varchar(255),
        email varchar(255) not null,
        name varchar(255) not null,
        password varchar(255),
        phone_number varchar(255),
        profile_image_url varchar(255),
        provider varchar(255),
        provider_id varchar(255),
        user_id varchar(255) not null,
        gender enum ('FEMALE','MALE','OTHER'),
        role enum ('ADMIN','CAREGIVER','GUEST','PARENT') not null,
        primary key (id)
    ) engine=InnoDB;

    alter table if exists tbl_care_facilities 
       add constraint UK3hjjssuyapo782v4xop9cr2ng unique (facility_code);

    alter table if exists tbl_chat_sessions 
       add constraint UKkwbpce69x8vs8s46ba8gjye7 unique (session_id);

    alter table if exists tbl_email_verification_token 
       add constraint UK54r1altk0sg47ofo806dvyxgr unique (token);

    alter table if exists tbl_health_record_types 
       add constraint UKp872n2rhas0464nuk8k7adr6a unique (name);

    alter table if exists tbl_hospital_like 
       add constraint UK7n0bpsjoplmq9diqrndarpy5g unique (hospital_id, user_id);

    alter table if exists tbl_notification_settings 
       add constraint UK95w8tup811r9fh0s7wvsjlg97 unique (user_id);

    alter table if exists tbl_notification_templates 
       add constraint UK3sooyj4tvid3mx1py70e56mvs unique (template_code);

    alter table if exists tbl_policies 
       add constraint UK6fc9lblhahwfjxol98xm3gywd unique (policy_code);

    alter table if exists tbl_policy_categories 
       add constraint UKmpybt30mg6cg7pxqmdfo4u3um unique (name);

    alter table if exists tbl_tag 
       add constraint UKl2viqa2nn516nlwrac59y4cpc unique (name);

    alter table if exists tbl_user 
       add constraint UKnpn1wf1yu1g5rjohbek375pp1 unique (email);

    alter table if exists care_facility_bookings 
       add constraint FKoh8g4b2l9n92gv3l7x1afmnml 
       foreign key (facility_id) 
       references tbl_care_facilities (id);

    alter table if exists notification_preferences 
       add constraint FKtq8149hrsxltm2a0sbhyk1lsa 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_chat_messages 
       add constraint FK1a3jnjg3ujcj8n1p0ouyhtpwk 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_chat_sessions 
       add constraint FKa2p6vpjdnthke38mtoqmlf499 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_child 
       add constraint FK94ujmxvi050yo65xauo3ekrxc 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_comment 
       add constraint FK3pcr8wwnnxrvnd761qmpqx9qn 
       foreign key (author_id) 
       references tbl_user (id);

    alter table if exists tbl_comment 
       add constraint FKiaoli5r54kuo9e35the496ei5 
       foreign key (parent_comment_id) 
       references tbl_comment (id);

    alter table if exists tbl_comment 
       add constraint FKi7k73l5d2j9cvam2bkepym80k 
       foreign key (post_id) 
       references tbl_post (id);

    alter table if exists tbl_email_verification_token 
       add constraint FK68vcalqvleencjhxuvavdq8o 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_health_record 
       add constraint FKkpsdmbj3xwxdw8i4wpfikmgnc 
       foreign key (child_id) 
       references tbl_child (id);

    alter table if exists tbl_health_record 
       add constraint FK93mrq5f81i6jq852nx31ksme5 
       foreign key (record_type_id) 
       references tbl_health_record_types (id);

    alter table if exists tbl_health_record 
       add constraint FKqomr15malntx5sft8l32x5bbu 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_health_record_attachments 
       add constraint FKm7iwergc85ovurp5q4xgp5n8s 
       foreign key (health_record_id) 
       references tbl_health_record (id);

    alter table if exists tbl_hospital_like 
       add constraint FKsuwxiiin7q4toreahee4jg4ct 
       foreign key (hospital_id) 
       references tbl_hospital (id);

    alter table if exists tbl_hospital_like 
       add constraint FKaw0scwoq98yl4mgflrjr54cia 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_hospital_review 
       add constraint FKpg2biorg2x0b0jlj2309bgx0f 
       foreign key (hospital_id) 
       references tbl_hospital (id);

    alter table if exists tbl_hospital_review 
       add constraint FKhl4j759rwx8i7h679g6cv1x3m 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_notification 
       add constraint FK17xlvi4d2o1r18carkq5kmd3c 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_notification_settings 
       add constraint FKhba3inhpi36hpwj8v2k6m4s3h 
       foreign key (user_id) 
       references tbl_user (id);

    alter table if exists tbl_policies 
       add constraint FKidigasv1f4je78japhcqjod83 
       foreign key (category_id) 
       references tbl_policy_categories (id);

    alter table if exists tbl_policy_documents 
       add constraint FKq9vn4dhtcah79kk5t15ctojqo 
       foreign key (policy_id) 
       references tbl_policies (id);

    alter table if exists tbl_post 
       add constraint FKfyrkyv3rigp2wbny02feexi1x 
       foreign key (author_id) 
       references tbl_user (id);

    alter table if exists tbl_post_tags 
       add constraint FKmf3p1y5q5no81aia3eurjr3fd 
       foreign key (tag_id) 
       references tbl_tag (id);

    alter table if exists tbl_post_tags 
       add constraint FKecu6x1cyk70umt8jh1usi4bn5 
       foreign key (post_id) 
       references tbl_post (id);

    alter table if exists tbl_reviews 
       add constraint FK6wc40s9ksn7oe612jygt9ev6x 
       foreign key (facility_id) 
       references tbl_care_facilities (id);

    alter table if exists tbl_reviews 
       add constraint FKpnf3fjbeg8xxuyj0hymwuwi 
       foreign key (user_id) 
       references tbl_user (id);

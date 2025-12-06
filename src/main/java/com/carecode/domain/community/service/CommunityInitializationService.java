package com.carecode.domain.community.service;

import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.PostCategory;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 더미 데이터 초기화 서비스
 * 서버 시작 시 자동으로 실행되어 더미 데이터를 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityInitializationService implements CommandLineRunner {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("커뮤니티 더미 데이터 초기화 시작");
        
        // testuser 생성 또는 확인
        User testUser = createTestUser();
        
        // 태그 생성
        List<Tag> tags = createTags();
        
        // 게시글 생성
        createPosts(testUser, tags);
        
        log.info("커뮤니티 더미 데이터 초기화 완료");
    }

    /**
     * 테스트 사용자 생성
     */
    private User createTestUser() {
        Optional<User> existingUser = userRepository.findByEmail("testuser@example.com");
        
        if (existingUser.isPresent()) {
            log.info("테스트 사용자가 이미 존재합니다: {}", existingUser.get().getEmail());
            return existingUser.get();
        }

        User testUser = User.builder()
                .email("testuser@example.com")
                .password(passwordEncoder.encode("password"))
                .name("testuser")
                .provider("LOCAL")
                .providerId("testuser")
                .role(UserRole.PARENT)
                .isActive(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(testUser);
        log.info("테스트 사용자 생성 완료: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * 태그 생성
     */
    private List<Tag> createTags() {
        List<String> tagNames = Arrays.asList("육아팁", "질문", "정보공유", "일상", "고민상담");
        List<Tag> tags = tagNames.stream()
                .map(tagName -> {
                    Optional<Tag> existingTag = tagRepository.findByName(tagName);
                    if (existingTag.isPresent()) {
                        return existingTag.get();
                    }
                    
                    Tag tag = new Tag(tagName, tagName + " 관련 게시글");
                    
                    Tag savedTag = tagRepository.save(tag);
                    log.info("태그 생성 완료: {}", savedTag.getName());
                    return savedTag;
                })
                .toList();

        return tags;
    }

    /**
     * 게시글 생성
     */
    private void createPosts(User testUser, List<Tag> tags) {
        // 이미 게시글이 있다면 생성하지 않음
        if (postRepository.count() > 0) {
            log.info("게시글이 이미 존재하므로 더미 데이터 생성을 건너뜁니다.");
            return;
        }

        List<Post> posts = Arrays.asList(
            // 육아팁 카테고리 (SHARE로 매핑)
            createPost("아이와 함께하는 재미있는 실내놀이 10가지", 
                "안녕하세요! 오늘은 비 오는 날 집에서 할 수 있는 재미있는 놀이들을 소개해드릴게요. 1. 종이컵 탑 쌓기 2. 양말 인형 만들기 3. 실내 캠핑 4. 요리 놀이 5. 그림 그리기 6. 퍼즐 맞추기 7. 노래 부르기 8. 춤추기 9. 책 읽기 10. 숨바꼭질 이렇게 10가지 놀이를 소개했는데, 아이들이 정말 좋아할 거예요! 특히 요리 놀이는 아이들이 가장 좋아하는 것 같아요. 간단한 샌드위치나 과일 샐러드를 함께 만들어보세요.", 
                testUser, PostCategory.SHARE, 156, 23, 1),

            createPost("아이 수면 교육 성공기 공유합니다", 
                "6개월 아기 수면 교육을 시작했는데, 생각보다 잘 되고 있어서 공유해드려요. 처음에는 정말 힘들었는데, 일정한 시간에 잠자리에 누우게 하고, 잠들기 전 루틴을 만들어주니까 점점 좋아지더라고요. 목욕 → 우유 → 책 읽기 → 잠자리 이 순서로 하니까 아이가 알아서 잠들 준비를 하기 시작했어요. 다른 분들도 도움이 되길 바라며 공유합니다!", 
                testUser, PostCategory.SHARE, 89, 15, 2),

            createPost("아이 장난감 정리하는 꿀팁", 
                "아이 장난감이 너무 많아서 정리하기 힘드시죠? 저도 그랬는데, 이렇게 하니까 훨씬 깔끔해졌어요. 1. 카테고리별로 분류하기 (블록, 인형, 책 등) 2. 투명한 박스에 정리하기 3. 아이가 쉽게 꺼낼 수 있게 하기 4. 정리하는 습관 들이기 5. 정기적으로 정리하기 이렇게 하니까 아이도 스스로 정리하는 습관이 생겼어요!", 
                testUser, PostCategory.SHARE, 234, 31, 3),

            createPost("아이와 함께하는 건강한 간식 만들기", 
                "아이들이 좋아하는 간식을 건강하게 만들어보세요! 오늘은 바나나 요구르트 아이스크림을 만들어봤어요. 재료: 바나나 2개, 요구르트 1컵, 꿀 1큰술 방법: 1. 바나나를 얼린다 2. 모든 재료를 블렌더에 넣는다 3. 부드럽게 갈아준다 4. 아이스크림 모양으로 만들어 얼린다 아이가 정말 좋아했어요! 설탕 대신 꿀을 사용해서 더 건강하답니다.", 
                testUser, PostCategory.SHARE, 167, 28, 4),

            createPost("아이 발달에 좋은 놀이 추천", 
                "아이 발달에 도움이 되는 놀이들을 소개해드릴게요. 1. 블록 쌓기 - 소근육 발달 2. 그림 그리기 - 창의력 발달 3. 숨바꼭질 - 대근육 발달 4. 역할놀이 - 사회성 발달 5. 퍼즐 - 문제해결력 발달 6. 노래 부르기 - 언어 발달 7. 춤추기 - 리듬감 발달 8. 책 읽기 - 집중력 발달 이렇게 다양한 놀이를 통해 아이의 전반적인 발달을 도와줄 수 있어요!", 
                testUser, PostCategory.SHARE, 198, 42, 5),

            // 질문 카테고리 (QUESTION으로 매핑)
            createPost("아이가 밤에 자꾸 깨는데 어떻게 해야 할까요?", 
                "2살 아이가 밤에 자꾸 깨서 고민이에요. 보통 새벽 2-3시에 깨서 우유를 달라고 하는데, 이게 습관이 된 것 같아요. 어떻게 하면 밤에 안 깨게 할 수 있을까요? 다른 분들은 어떻게 하시나요? 조언 부탁드려요!", 
                testUser, PostCategory.QUESTION, 312, 18, 6),

            createPost("아이가 밥을 안 먹어요. 어떻게 해야 할까요?", 
                "3살 아이가 밥을 정말 안 먹어요. 간식은 잘 먹는데 정작 밥은 몇 숟가락만 먹고 끝내요. 영양소가 부족할까봐 걱정이에요. 아이가 밥을 잘 먹게 하는 방법이 있을까요? 장난감으로 유혹하거나 TV를 보면서 먹이기도 해봤는데, 이게 좋은 방법은 아닌 것 같아요. 조언 부탁드려요!", 
                testUser, PostCategory.QUESTION, 445, 25, 7),

            createPost("아이가 친구와 잘 어울리지 못해요", 
                "4살 아이가 유치원에서 친구들과 잘 어울리지 못하는 것 같아요. 집에서는 활발한데, 유치원에서는 조용히 혼자 놀고 있다고 해요. 사회성이 부족한 걸까요? 어떻게 하면 아이가 친구들과 잘 어울리게 될까요? 고민이에요.", 
                testUser, PostCategory.QUESTION, 278, 19, 8),

            createPost("아이 화장실 교육 언제부터 시작해야 할까요?", 
                "아이가 2살 6개월인데, 화장실 교육을 언제부터 시작해야 할까요? 지금은 기저귀를 사용하고 있는데, 언제부터 시작하는 게 좋을까요? 준비가 되었다는 신호가 있을까요? 경험담이나 조언 부탁드려요!", 
                testUser, PostCategory.QUESTION, 189, 12, 9),

            createPost("아이가 스마트폰을 너무 좋아해요", 
                "5살 아이가 스마트폰을 너무 좋아해서 고민이에요. 하루에 2-3시간씩 보려고 하는데, 이게 정말 문제가 될까요? 시력이나 발달에 영향을 줄까봐 걱정이에요. 어떻게 하면 스마트폰 사용을 줄일 수 있을까요?", 
                testUser, PostCategory.QUESTION, 356, 33, 10),

            // 정보공유 카테고리 (SHARE로 매핑)
            createPost("좋은 어린이집 찾는 꿀팁 공유", 
                "어린이집을 찾을 때 체크해야 할 포인트들을 정리해봤어요. 1. 위치와 거리 2. 교사 대 아동 비율 3. 프로그램과 커리큘럼 4. 위생 상태 5. 안전 시설 6. 급식 메뉴 7. 학부모 참여도 8. 비용과 등록금 9. 운영 시간 10. 교사 자격증 이렇게 체크해보시면 좋을 것 같아요!", 
                testUser, PostCategory.SHARE, 423, 67, 11),

            createPost("아이 건강검진 일정표", 
                "아이 건강검진 일정을 정리해봤어요. 출생 후: 1주일, 1개월, 4개월, 6개월, 9개월, 12개월, 15개월, 18개월, 24개월, 30개월, 36개월, 42개월, 48개월, 54개월, 60개월 이렇게 정기적으로 받아야 해요. 각 시기별로 체크하는 항목도 다르니 꼭 받으시길 바라요!", 
                testUser, PostCategory.SHARE, 298, 45, 12),

            createPost("아이 예방접종 일정표", 
                "아이 예방접종 일정을 정리해봤어요. 출생 시: B형간염 1차, BCG 2개월: B형간염 2차, DTaP 1차, IPV 1차, Hib 1차, PCV 1차 4개월: DTaP 2차, IPV 2차, Hib 2차, PCV 2차 6개월: B형간염 3차, DTaP 3차, IPV 3차, Hib 3차, PCV 3차 12개월: MMR 1차, 수두 1차, 일본뇌염 1차 이렇게 받아야 해요!", 
                testUser, PostCategory.SHARE, 345, 52, 13),

            createPost("아이 장난감 구매 가이드", 
                "아이 연령별로 추천하는 장난감을 정리해봤어요. 0-6개월: 모빌, 딸랑이, 촉감놀이 6-12개월: 블록, 인형, 그림책 1-2세: 퍼즐, 역할놀이, 그리기 도구 2-3세: 레고, 보드게임, 운동놀이 3-4세: 창작놀이, 과학놀이, 음악놀이 4-5세: 복잡한 퍼즐, 전략게임, 예술놀이 이렇게 연령에 맞는 장난감을 선택하시면 좋아요!", 
                testUser, PostCategory.SHARE, 267, 38, 14),

            createPost("아이 영양소별 필수 음식", 
                "아이 발달에 필요한 영양소별 필수 음식을 정리해봤어요. 단백질: 계란, 생선, 고기, 콩류 칼슘: 우유, 요구르트, 치즈, 두부 철분: 시금치, 고기, 콩류, 견과류 비타민C: 오렌지, 키위, 브로콜리, 토마토 비타민D: 생선, 계란, 버섯, 햇빛 오메가3: 생선, 견과류, 아보카도 이렇게 골고루 먹여주세요!", 
                testUser, PostCategory.SHARE, 312, 41, 15),

            // 일상 카테고리 (GENERAL로 매핑)
            createPost("오늘 아이와 함께한 특별한 하루", 
                "오늘은 아이와 함께 동물원에 갔어요. 코끼리를 보고 아이가 정말 신기해했는데, 코끼리가 코로 물을 뿜는 걸 보고는 깔깔 웃었어요. 점심은 동물원 안에서 먹었는데, 아이가 정말 좋아했어요. 오후에는 놀이터에서 놀았는데, 그네를 타면서 더 높이! 라고 외치더라고요. 정말 즐거운 하루였어요!",
                testUser, PostCategory.GENERAL, 156, 29, 16),

            createPost("아이가 처음으로 말을 했어요!", 
                "오늘은 정말 특별한 날이에요! 아이가 처음으로 '엄마' 라고 말했어요! 정말 감동적이었어요. 10개월이 되면서 옹알이를 많이 했는데, 오늘 갑자기 '엄마' 라고 똑똑하게 말하더라고요. 눈물이 날 뻔했어요. 이 순간을 잊지 못할 것 같아요.",
                testUser, PostCategory.GENERAL, 234, 78, 17),

            createPost("아이와 함께한 요리 시간", 
                "오늘은 아이와 함께 쿠키를 만들어봤어요. 반죽을 만드는 것부터 아이가 정말 신기해했는데, 밀가루를 만지면서 부드러워! 라고 하더라고요. 쿠키 모양을 찍을 때는 정말 집중해서 했어요. 구워진 쿠키를 먹을 때는 맛있어! 라고 하면서 정말 좋아했어요. 함께 만든 쿠키가 더 맛있었어요!",
                testUser, PostCategory.GENERAL, 189, 34, 18),

            createPost("아이의 첫 걸음마", 
                "오늘은 아이가 처음으로 혼자 걸었어요! 13개월이 되면서 서는 연습을 많이 했는데, 오늘 갑자기 한 걸음, 두 걸음 걸었어요! 정말 놀라웠어요. 아이도 자신이 걸었다는 걸 알았는지 깔깔 웃었어요. 이제 걷기 연습을 더 많이 해야겠어요.", 
                testUser, PostCategory.GENERAL, 298, 89, 19),

            createPost("아이와 함께한 공원 산책", 
                "오늘은 날씨가 좋아서 아이와 함께 공원에 갔어요. 단풍이 예쁘게 물들어서 정말 아름다웠어요. 아이는 낙엽을 밟으면서 바스락바스락 소리를 내며 좋아했어요. 오리들도 보고, 나무도 만져보고, 정말 즐거운 시간이었어요. 가을 산책이 정말 좋네요!",
                testUser, PostCategory.GENERAL, 167, 23, 20),

            // 고민상담 카테고리 (QUESTION으로 매핑)
            createPost("아이와의 소통이 어려워요", 
                "아이가 4살인데, 요즘 소통이 어려워요. 뭔가 하고 싶은 것 같은데 말로 표현을 못해서 짜증을 내고, 저도 이해를 못해서 화를 내게 되고... 이렇게 되면 아이가 더 울고, 저도 스트레스받고... 어떻게 하면 아이와 더 잘 소통할 수 있을까요? 조언 부탁드려요.", 
                testUser, PostCategory.QUESTION, 445, 56, 21),

            createPost("육아하면서 직장생활 하기 힘들어요", 
                "아이가 2살인데, 육아하면서 직장생활을 하기가 정말 힘들어요. 아이가 아프면 회사에 못 가고, 야근도 못 하고... 회사에서는 이해해주지 못하고, 집에서는 아이를 제대로 돌봐주지 못하는 것 같아서 죄송하고... 어떻게 하면 육아와 직장생활을 잘 병행할 수 있을까요?", 
                testUser, PostCategory.QUESTION, 523, 67, 22),

            createPost("아이가 형제와 자꾸 싸워요", 
                "5살 큰아이와 3살 작은아이를 키우고 있는데, 자꾸 싸워요. 장난감 때문에, TV 보기 때문에, 먹을 것 때문에... 하루에도 몇 번씩 싸우는데, 저도 지쳐요. 어떻게 하면 형제가 사이좋게 지낼 수 있을까요? 조언 부탁드려요.", 
                testUser, PostCategory.QUESTION, 378, 45, 23),

            createPost("아이 교육에 대한 고민", 
                "아이가 6살인데, 요즘 아이 교육에 대한 고민이 많아요. 다른 아이들은 영어, 수학, 음악 등 여러 학원을 다니는데, 우리 아이는 아직 아무것도 안 시키고 있어요. 시키면 아이가 힘들까봐 걱정이고, 안 시키면 뒤처질까봐 걱정이에요. 어떻게 하면 좋을까요?", 
                testUser, PostCategory.QUESTION, 412, 52, 24),

            createPost("아이와의 시간이 부족해요", 
                "직장생활 때문에 아이와 함께하는 시간이 정말 부족해요. 아침에 일찍 출근하고, 저녁에 늦게 퇴근해서 아이를 볼 수 있는 시간이 하루에 2-3시간밖에 없어요. 아이가 저를 그리워하는 것 같고, 저도 아이가 보고 싶은데... 어떻게 하면 아이와 더 많은 시간을 보낼 수 있을까요?", 
                testUser, PostCategory.QUESTION, 356, 48, 25),

            // 추가 게시글들
            createPost("아이와 함께하는 실외놀이 추천", 
                "날씨가 좋아지면서 실외놀이를 할 수 있는 기회가 많아졌어요. 추천하는 실외놀이들을 소개해드릴게요. 1. 공원에서 놀기 2. 자전거 타기 3. 공놀이 4. 모래놀이 5. 물놀이 6. 숲 탐험 7. 벼룩시장 가기 8. 동물원 가기 9. 박물관 가기 10. 도서관 가기 이렇게 다양한 실외활동을 통해 아이의 체력과 탐구심을 키워줄 수 있어요!", 
                testUser, PostCategory.SHARE, 145, 22, 26),

            createPost("아이 독서 습관 들이기", 
                "아이에게 독서 습관을 들이는 방법을 공유해드릴게요. 1. 매일 정해진 시간에 책 읽기 2. 아이가 좋아하는 책 선택하기 3. 목소리 톤을 바꿔가며 읽기 4. 그림을 보면서 이야기하기 5. 질문을 하며 읽기 6. 책을 재미있게 읽기 7. 도서관에 자주 가기 8. 책을 선물로 주기 이렇게 하면 아이가 자연스럽게 책을 좋아하게 될 거예요!", 
                testUser, PostCategory.SHARE, 178, 31, 27),

            createPost("아이 스트레스 해소 방법", 
                "아이들도 스트레스를 받는다는 걸 아시나요? 아이의 스트레스를 해소하는 방법들을 소개해드릴게요. 1. 마사지 해주기 2. 따뜻한 목욕 3. 좋아하는 음식 먹기 4. 놀이를 통한 스트레스 해소 5. 대화하기 6. 산책하기 7. 음악 듣기 8. 그림 그리기 9. 춤추기 10. 포옹하기 이렇게 해주면 아이의 스트레스가 해소될 거예요!", 
                testUser, PostCategory.SHARE, 198, 28, 28),

            createPost("아이와 함께하는 요리 레시피", 
                "아이와 함께 만들 수 있는 간단한 요리 레시피를 공유해드릴게요. 1. 과일 샐러드 - 아이가 좋아하는 과일을 썰어서 섞기 2. 샌드위치 - 빵에 좋아하는 재료를 올리기 3. 팬케이크 - 반죽을 섞고 팬에 부치기 4. 과일 스무디 - 과일과 우유를 블렌더에 갈기 5. 쿠키 - 반죽을 만들고 모양 찍기 이렇게 하면 아이가 요리의 재미를 알게 될 거예요!", 
                testUser, PostCategory.SHARE, 167, 25, 29),

            createPost("아이 발달 체크리스트", 
                "아이 연령별 발달 체크리스트를 정리해봤어요. 12개월: 혼자 서기, 첫 단어 말하기 18개월: 혼자 걷기, 간단한 지시 따르기 24개월: 2단어 문장, 그림 그리기 30개월: 3단어 문장, 간단한 게임 36개월: 대화하기, 친구와 놀기 48개월: 복잡한 문장, 규칙 이해하기 60개월: 읽기 쓰기, 논리적 사고 이렇게 체크해보시면 좋을 것 같아요!", 
                testUser, PostCategory.SHARE, 234, 39, 30)
        );

        postRepository.saveAll(posts);
        log.info("{}개의 게시글 생성 완료", posts.size());
    }

    /**
     * 게시글 생성 헬퍼 메서드
     */
    private Post createPost(String title, String content, User author, PostCategory category, 
                           int viewCount, int likeCount, int daysAgo) {
        return Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .authorName(author.getName())
                .category(category)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .createdAt(LocalDateTime.now().minusDays(daysAgo))
                .updatedAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
    }
}

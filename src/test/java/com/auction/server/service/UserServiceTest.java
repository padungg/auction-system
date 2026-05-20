package com.auction.server.service;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.UserDAO;
import com.auction.server.service.UserService;
import com.auction.server.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho UserService — sử dụng stub thủ công (không cần Mockito).
 * Tương thích với Java 25.
 */
@DisplayName("UserService Tests")
class UserServiceTest {

    // STUB: UserDAO in-memory thay vì Mockito mock
    static class UserDAOStub implements UserDAO {
        private final Map<String, User> byUsername = new HashMap<>();
        private final Map<String, User> byId = new HashMap<>();
        private boolean saveReturnValue = true; // điều khiển kết quả save()

        void addUser(User u) {
            byUsername.put(u.getUsername(), u);
            byId.put(u.getId(), u);
        }

        void setSaveReturnValue(boolean v) { this.saveReturnValue = v; }

        @Override public User findByUsername(String username) { return byUsername.get(username); }
        @Override public User findById(String id) { return byId.get(id); }
        @Override public boolean existsByUsername(String username) { return byUsername.containsKey(username); }
        @Override public boolean save(User user) {
            if (saveReturnValue) addUser(user);
            return saveReturnValue;
        }
        public boolean update(User user) { addUser(user); return true; }
        public List<User> findAll() { return new ArrayList<>(byUsername.values()); }
        public boolean delete(String id) { User u = byId.remove(id); if(u!=null) byUsername.remove(u.getUsername()); return u!=null; }
        @Override public User findFirstByRole(com.auction.model.entity.UserRole role) { return byUsername.values().stream().filter(u -> u.getRole() == role).findFirst().orElse(null); }
    }

    private UserDAOStub userDAO;
    private UserService userService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAOStub();
        userService = new UserService(userDAO);

        activeUser = new User(
                "uid-001", "alice", "pass123",
                "alice@example.com", "Alice Nguyen", "0901234567",
                "HN", true, UserRole.MEMBER, 5_000_000.0, null, 0.0
        );
        userDAO.addUser(activeUser);
    }

    // LOGIN TESTS
    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("TC-LOGIN-01: Đăng nhập thành công → SUCCESS + UserResponseDTO")
        void login_success() {
            Response res = userService.login(new LoginDTO("alice", "pass123"));
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertInstanceOf(UserResponseDTO.class, res.getPayload());
            UserResponseDTO payload = (UserResponseDTO) res.getPayload();
            assertEquals("alice", payload.getUsername());
            assertEquals("alice@example.com", payload.getEmail());
            assertEquals(UserRole.MEMBER, payload.getRole());
            assertEquals(5_000_000.0, payload.getBalance(), 0.001);
        }

        @Test
        @DisplayName("TC-LOGIN-02: DTO null → BAD_REQUEST")
        void login_nullDto() {
            assertEquals(ResponseStatus.BAD_REQUEST, userService.login(null).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-03: Username rỗng → BAD_REQUEST")
        void login_emptyUsername() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    userService.login(new LoginDTO("  ", "pass123")).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-04: Password rỗng → BAD_REQUEST")
        void login_emptyPassword() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    userService.login(new LoginDTO("alice", "  ")).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-05: Username không tồn tại → UNAUTHORIZED")
        void login_userNotFound() {
            assertEquals(ResponseStatus.UNAUTHORIZED,
                    userService.login(new LoginDTO("ghost", "pass123")).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-06: Sai mật khẩu → UNAUTHORIZED")
        void login_wrongPassword() {
            assertEquals(ResponseStatus.UNAUTHORIZED,
                    userService.login(new LoginDTO("alice", "wrong")).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-07: Tài khoản bị khóa → UNAUTHORIZED + message 'khóa'")
        void login_accountLocked() {
            activeUser.setActive(false);
            Response res = userService.login(new LoginDTO("alice", "pass123"));
            assertEquals(ResponseStatus.UNAUTHORIZED, res.getStatus());
            assertTrue(res.getMessage().contains("khóa"));
        }

        @Test
        @DisplayName("TC-LOGIN-08: Username null → BAD_REQUEST")
        void login_nullUsername() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    userService.login(new LoginDTO(null, "pass123")).getStatus());
        }

        @Test
        @DisplayName("TC-LOGIN-09: Password null → BAD_REQUEST")
        void login_nullPassword() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                    userService.login(new LoginDTO("alice", null)).getStatus());
        }
    }

    // REGISTER TESTS
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterDTO validDto;

        @BeforeEach
        void setUp() {
            validDto = new RegisterDTO(
                    "newuser", "securePass1", "newuser@mail.com",
                    "New User", "0900000000", "HCM"
            );
        }

        @Test
        @DisplayName("TC-REG-01: Đăng ký thành công → SUCCESS + UserResponseDTO với role MEMBER")
        void register_success() {
            Response res = userService.register(validDto);
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertInstanceOf(UserResponseDTO.class, res.getPayload());
            UserResponseDTO payload = (UserResponseDTO) res.getPayload();
            assertEquals("newuser", payload.getUsername());
            assertEquals(UserRole.MEMBER, payload.getRole());
            assertEquals(0.0, payload.getBalance(), 0.001);
            // Kiểm tra user đã được lưu vào stub DB
            assertNotNull(userDAO.findByUsername("newuser"));
        }

        @Test
        @DisplayName("TC-REG-02: DTO null → BAD_REQUEST")
        void register_nullDto() {
            assertEquals(ResponseStatus.BAD_REQUEST, userService.register(null).getStatus());
        }

        @Test
        @DisplayName("TC-REG-03: Username rỗng → ValidationException")
        void register_blankUsername() {
            validDto.setUsername("  ");
            assertThrows(ValidationException.class, () -> userService.register(validDto));
        }

        @Test
        @DisplayName("TC-REG-04: Password rỗng → ValidationException")
        void register_blankPassword() {
            validDto.setPassword("");
            assertThrows(ValidationException.class, () -> userService.register(validDto));
        }

        @Test
        @DisplayName("TC-REG-05: Email không hợp lệ → ValidationException")
        void register_invalidEmail() {
            validDto.setEmail("not-an-email");
            assertThrows(ValidationException.class, () -> userService.register(validDto));
        }

        @Test
        @DisplayName("TC-REG-06: Email null → ValidationException")
        void register_nullEmail() {
            validDto.setEmail(null);
            assertThrows(ValidationException.class, () -> userService.register(validDto));
        }

        @Test
        @DisplayName("TC-REG-07: Username đã tồn tại → BAD_REQUEST")
        void register_duplicateUsername() {
            validDto.setUsername("alice"); // alice đã có trong DB
            Response res = userService.register(validDto);
            assertEquals(ResponseStatus.BAD_REQUEST, res.getStatus());
            assertTrue(res.getMessage().contains("đã tồn tại"));
        }

        @Test
        @DisplayName("TC-REG-08: DAO save thất bại → ERROR")
        void register_daoSaveFails() {
            userDAO.setSaveReturnValue(false);
            Response res = userService.register(validDto);
            assertEquals(ResponseStatus.ERROR, res.getStatus());
        }

        @Test
        @DisplayName("TC-REG-09: fullName null → vẫn thành công")
        void register_nullFullName_ok() {
            validDto.setFullName(null);
            assertEquals(ResponseStatus.SUCCESS, userService.register(validDto).getStatus());
        }

        @Test
        @DisplayName("TC-REG-10: Các format email hợp lệ khác nhau")
        void register_validEmailFormats() {
            String[] emails = {"a@b.vn", "user.name@domain.org", "x@y.com"};
            int i = 0;
            for (String email : emails) {
                RegisterDTO dto = new RegisterDTO(
                        "user" + i++, "pass123", email, "Name", "", ""
                );
                assertEquals(ResponseStatus.SUCCESS, userService.register(dto).getStatus(),
                        "Email hợp lệ phải đăng ký được: " + email);
            }
        }
    }

    @Nested
    @DisplayName("deposit() và withdraw()")
    class TransactionTests {

        @Test
        @DisplayName("TC-USER-DEP-01: userId null → UNAUTHORIZED")
        void deposit_nullUserId() {
            assertEquals(ResponseStatus.UNAUTHORIZED,
                userService.deposit(null, 100_000.0).getStatus());
        }

        @Test
        @DisplayName("TC-USER-DEP-02: amount <= 0 → BAD_REQUEST")
        void deposit_invalidAmount() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                userService.deposit("uid-001", 0).getStatus());
            assertEquals(ResponseStatus.BAD_REQUEST,
                userService.deposit("uid-001", -500.0).getStatus());
        }

        @Test
        @DisplayName("TC-USER-DEP-03: Nạp tiền thành công → số dư tăng")
        void deposit_success() {
            Response res = userService.deposit("uid-001", 1_000_000.0);
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertEquals(6_000_000.0, activeUser.getBalance(), 0.001);
        }

        @Test
        @DisplayName("TC-USER-WD-01: Rút tiền khi số dư không đủ → BAD_REQUEST")
        void withdraw_insufficientBalance() {
            assertEquals(ResponseStatus.BAD_REQUEST,
                userService.withdraw("uid-001", 10_000_000.0).getStatus());
        }

        @Test
        @DisplayName("TC-USER-WD-02: Rút tiền thành công → số dư giảm")
        void withdraw_success() {
            Response res = userService.withdraw("uid-001", 1_000_000.0);
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertEquals(4_000_000.0, activeUser.getBalance(), 0.001);
        }
    }

    @Nested
    @DisplayName("lockUser() và unlockUser()")
    class LockUnlockTests {

        @Test
        @DisplayName("TC-USER-LOCK-01: userId null → BAD_REQUEST")
        void lockUser_nullId() {
            assertEquals(ResponseStatus.BAD_REQUEST, userService.lockUser(null).getStatus());
        }

        @Test
        @DisplayName("TC-USER-LOCK-02: userId không tồn tại → NOT_FOUND")
        void lockUser_notFound() {
            assertEquals(ResponseStatus.NOT_FOUND, userService.lockUser("ghost-id").getStatus());
        }

        @Test
        @DisplayName("TC-USER-LOCK-03: Khóa thành công → isActive = false")
        void lockUser_success() {
            Response res = userService.lockUser("uid-001");
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertFalse(activeUser.isActive());
        }

        @Test
        @DisplayName("TC-USER-LOCK-04: Mở khóa thành công → isActive = true")
        void unlockUser_success() {
            activeUser.setActive(false);
            Response res = userService.unlockUser("uid-001");
            assertEquals(ResponseStatus.SUCCESS, res.getStatus());
            assertTrue(activeUser.isActive());
        }
    }
}

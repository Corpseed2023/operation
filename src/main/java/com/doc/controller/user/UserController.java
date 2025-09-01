    package com.doc.controller.user;

    import com.doc.dto.user.UserRequestDto;
    import com.doc.dto.user.UserResponseDto;
    import com.doc.service.UserService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.responses.ApiResponse;
    import io.swagger.v3.oas.annotations.responses.ApiResponses;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import jakarta.validation.Valid;
    import java.util.List;

    @RestController
    @RequestMapping("/api/users")
    public class UserController {

        @Autowired
        private UserService userService;

        @Operation(summary = "Create a new user")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "User created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "409", description = "User with email already exists")
        })
        @PostMapping
        public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto requestDto) {
            UserResponseDto responseDto = userService.createUser(requestDto);
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        }

        @Operation(summary = "Get a user by ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "User found"),
                @ApiResponse(responseCode = "404", description = "User not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
            UserResponseDto responseDto = userService.getUserById(id);
            return ResponseEntity.ok(responseDto);
        }

        @Operation(summary = "Get all users with optional filtering and pagination")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "List of users retrieved"),
                @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
        })
        @GetMapping
        public ResponseEntity<List<UserResponseDto>> getAllUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestParam(required = false) String fullName,
                @RequestParam(required = false) String email,
                @RequestParam(required = false) Boolean isManager) {
            List<UserResponseDto> users = userService.getAllUsers(page, size, fullName, email, isManager);
            return ResponseEntity.ok(users);
        }

        @Operation(summary = "Update a user by ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "User updated successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "404", description = "User not found"),
                @ApiResponse(responseCode = "409", description = "User with email already exists")
        })
        @PutMapping("/{id}")
        public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto requestDto) {
            UserResponseDto responseDto = userService.updateUser(id, requestDto);
            return ResponseEntity.ok(responseDto);
        }

        @Operation(summary = "Delete a user by ID (soft delete)")
        @ApiResponses({
                @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                @ApiResponse(responseCode = "404", description = "User not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        }
    }
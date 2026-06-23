package wearweather.wearweather_server.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 30)
    private String nickname;

    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "sensitivity_offset")
    private Float sensitivityOffset = 0.0f;

    @Enumerated(EnumType.STRING)
    @Column(name = "style_preference", length = 20)
    private StylePreference stylePreference;

    public User(UUID id, String email) {
        this.id = id;
        this.email = email;
        this.nickname = defaultNickname(email);
    }

    public void updateProfile(String nickname, Integer age, Gender gender, Float sensitivityOffset, StylePreference stylePreference) {
        if (nickname != null && !nickname.isBlank()) {
            if (nickname.length() > 30) {
                throw new IllegalArgumentException("Nickname cannot exceed 30 characters");
            }
            this.nickname = nickname;
        }
        this.age = age;
        if (gender != null) {
            this.gender = gender;
        }
        if (sensitivityOffset != null) {
            this.sensitivityOffset = sensitivityOffset;
        }
        if (stylePreference != null) {
            this.stylePreference = stylePreference;
        }
    }

    private String defaultNickname(String email) {
        if (email == null || email.isBlank()) {
            return "user";
        }

        String localPart = email.split("@", 2)[0];
        String nickname = localPart.isBlank() ? "user" : localPart;
        return nickname.length() > 30 ? nickname.substring(0, 30) : nickname;
    }
}

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

    public User(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public void updateProfile(String nickname, Integer age) {
        this.nickname = nickname;
        this.age = age;
    }
}

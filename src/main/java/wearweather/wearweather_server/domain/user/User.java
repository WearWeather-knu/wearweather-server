package wearweather.wearweather_server.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Enumerated(EnumType.STRING) // DB에 문자열(MALE/FEMALE)로 저장되도록 설정
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sensitivity_offset", nullable = false)
    private Float sensitivityOffset;

    @Builder
    public User(UUID id, Gender gender, Integer age, Float sensitivityOffset) {
        this.id = id;
        this.gender = gender;
        this.age = age;
        this.sensitivityOffset = sensitivityOffset;
    }

    public void updateProfile(Gender gender, Integer age, Float sensitivityOffset) {
        this.gender = gender;
        this.age = age;
        this.sensitivityOffset = sensitivityOffset;
    }
}

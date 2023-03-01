package us.obviously.itmo.prog.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;

public class Person {
    private String name; //Поле не может быть null, Строка не может быть пустой
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    private java.time.ZonedDateTime birthday; //Поле не может быть null
    private Color eyeColor; //Поле может быть null
    private Color hairColor; //Поле может быть null
    private Country nationality; //Поле может быть null

    public static Builder newBuilder() {
        return new Person().new Builder();
    }

    public Country getNationality() {
        return nationality;
    }

    public Color getHairColor() {
        return hairColor;
    }

    public Color getEyeColor() {
        return eyeColor;
    }

    public ZonedDateTime getBirthday() {
        return birthday;
    }

    public String getName() {
        return name;
    }

    public class Builder {
        public Builder setName(String name) {
            Person.this.name = name;

            return this;
        }

        public Builder setBirthday(ZonedDateTime birthday) {
            Person.this.birthday = birthday;

            return this;
        }

        public Builder setEyeColor(Color eyeColorЯ) {
            Person.this.eyeColor = eyeColor;

            return this;
        }

        public Builder setHairColor(Color hairColor) {
            Person.this.hairColor = hairColor;

            return this;
        }

        public Builder setNationality(Country nationality) {
            Person.this.nationality = nationality;

            return this;
        }

        public Person build() {
            return Person.this;
        }
    }
}

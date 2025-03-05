package com.backend.crosswords.corpus.dto;

public class CreateTagDTO {
    private String name;

    public CreateTagDTO(String name) {
        this.name = name;
    }

    public CreateTagDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CreateTagDTO{" +
                "name='" + name + '\'' +
                '}';
    }
}

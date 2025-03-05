package com.backend.crosswords.corpus.dto;

public class TagDTO {
    private String name;

    public TagDTO(String name) {
        this.name = name;
    }

    public TagDTO() {
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

package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Package;
import com.backend.crosswords.corpus.models.PackageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, PackageId> {
    List<Package> getAllByOwner(User user);
}

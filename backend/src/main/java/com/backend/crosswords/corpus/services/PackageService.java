package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Package;
import com.backend.crosswords.corpus.models.PackageId;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.PackageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class PackageService {
    private final PackageRepository packageRepository;

    public PackageService(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public void createPackage(String name, User user) {
        // Ограничение на максимальный размер имени папки, больше 225 символов - запрещено
        if (name.length() > 255) {
            throw new IllegalArgumentException("This name is too long, max 255 characters!");
        }
        var packageId = new PackageId(name, user.getId());
        if (packageRepository.existsById(packageId)) {
            throw new IllegalArgumentException("You are trying to create a package with already existing name for this user!");
        }
        Package newPackage = new Package(packageId, user);
        packageRepository.save(newPackage);
    }

    public void deletePackageByName(String name, User user) {
        if (Objects.equals(name, Package.favouritesName)) {
            throw new IllegalArgumentException("You cant delete a package with this name!");
        }
        var packageId = new PackageId(name, user.getId());
        Package pack = packageRepository.findById(packageId).orElseThrow();
        packageRepository.delete(pack);
    }

    public Package getPackageByName(String name, User user) {
        var packageId = new PackageId(name, user.getId());
        var check = packageRepository.findById(packageId);
        if (check.isEmpty()) {
            throw new NoSuchElementException("There is no packages with such name!");
        }
        return check.get();
    }

    public void addDocIntoPackageByName(User user, DocMeta doc, String packageName) {
        var pack = this.getPackageByName(packageName, user);
        pack.getDocs().add(doc);
        packageRepository.save(pack);
    }

    public void removeDocFromPackages(DocMeta docMeta) {
        Iterator<Package> iterator = docMeta.getPackages().iterator();
        while (iterator.hasNext()) {
            Package pack = iterator.next();
            pack.getDocs().remove(docMeta);
            iterator.remove();
            packageRepository.save(pack);
        }
    }

    public void removeDocFromPackageByName(User user, DocMeta doc, String packageName) {
        var check = packageRepository.findById(new PackageId(packageName, user.getId()));
        if (check.isEmpty()) {
            throw new NoSuchElementException("There is no packages with such name!");
        }
        var pack = check.get();
        pack.getDocs().remove(doc);
        packageRepository.save(pack);
    }

    public Boolean checkDocInFavourites(User user, DocMeta doc) {
        var pack = packageRepository.findById(new PackageId(Package.favouritesName, user.getId())).orElseThrow();
        return pack.getDocs().contains(doc);
    }

    public void addDocToFavourites(User user, DocMeta doc) {
        this.addDocIntoPackageByName(user, doc, Package.favouritesName);
    }

    public void removeDocFromFavourites(User user, DocMeta doc) {
        var pack = packageRepository.findById(new PackageId(Package.favouritesName, user.getId())).orElseThrow();
        pack.getDocs().remove(doc);
        packageRepository.save(pack);
    }
}

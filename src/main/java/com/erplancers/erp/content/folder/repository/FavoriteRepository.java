package com.erplancers.erp.content.folder.repository;

import com.erplancers.erp.content.folder.domain.Favorite;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface FavoriteRepository extends ErpRepository<Favorite> {
    List<Favorite> findByFavoriteNameContainsIgnoreCase(String favoriteName);
}

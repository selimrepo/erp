package com.erplancers.erp.content.folder.repository;

import com.erplancers.erp.content.folder.domain.Folder;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface FolderRepository extends ErpRepository<Folder> {
    List<Folder> findByName(String name);
}

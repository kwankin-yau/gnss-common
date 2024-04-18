/*
 * Copyright (c) 2019-2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.db;

import com.lucendar.gnss.types.CreateMmReq;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MultiMediaDao {

    void createMedia(CreateMmReq req);
}

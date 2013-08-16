package com.buschmais.jqassistant.core.store.impl.dao;

import com.buschmais.jqassistant.core.model.api.descriptor.Descriptor;

import java.util.HashMap;
import java.util.Map;

public class DescriptorCache {

    private final Map<Long, Descriptor> cache = new HashMap<Long, Descriptor>();

    public <T extends Descriptor> void put(T descriptor) {
        Long key = Long.valueOf(descriptor.getId());
        this.cache.put(key, descriptor);
    }

    @SuppressWarnings("unchecked")
    public <T extends Descriptor> T findBy(Long id) {
        return (T) cache.get(id);
    }

    public Iterable<Descriptor> getDescriptors() {
        return cache.values();
    }

    public void clear() {
        this.cache.clear();
    }
}
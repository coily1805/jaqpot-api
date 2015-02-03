/*
 *
 * JAQPOT Quattro
 *
 * JAQPOT Quattro and the components shipped with it (web applications and beans)
 * are licenced by GPL v3 as specified hereafter. Additional components may ship
 * with some other licence as will be specified therein.
 *
 * Copyright (C) 2014-2015 KinkyDesign (Charalampos Chomenidis, Pantelis Sopasakis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Source code:
 * The source code of JAQPOT Quattro is available on github at:
 * https://github.com/KinkyDesign/JaqpotQuattro
 * All source files of JAQPOT Quattro that are stored on github are licenced
 * with the aforementioned licence. 
 */
package org.jaqpot.core.model;

import java.util.Set;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 *
 * @author chung
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public abstract class JaqpotEntity {

    /**
     * Identifier of the entity.
     */  
    private String id;
    /**
     * Metadata of the entity.
     */
    private MetaInfo meta;
    /**
     * Set of ontological characterizations.
     */
    private Set<String> ontologicalClasses;

    public JaqpotEntity() {
    }

    public JaqpotEntity(String id) {
        this.id = id;
    }
            
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
        
    public MetaInfo getMeta() {
        return meta;
    }

    public void setMeta(MetaInfo meta) {
        this.meta = meta;
    }

    public Set<String> getOntologicalClasses() {
        return ontologicalClasses;
    }

    public void setOntologicalClasses(Set<String> ontologicalClasses) {
        this.ontologicalClasses = ontologicalClasses;
    }

}
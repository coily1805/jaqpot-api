/*
 *
 * JAQPOT Quattro
 *
 * JAQPOT Quattro and the components shipped with it, in particular:
 * (i)   JaqpotCoreServices
 * (ii)  JaqpotAlgorithmServices
 * (iii) JaqpotDB
 * (iv)  JaqpotDomain
 * (v)   JaqpotEAR
 * are licensed by GPL v3 as specified hereafter. Additional components may ship
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
 * All source files of JAQPOT Quattro that are stored on github are licensed
 * with the aforementioned licence. 
 */
package org.jaqpot.core.data;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeMap;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.jaqpot.core.annotations.MongoDB;
import org.jaqpot.core.db.entitymanager.JaqpotEntityManager;
import org.jaqpot.core.model.dto.dataset.DataEntry;
import org.jaqpot.core.model.dto.dataset.Dataset;
import org.jaqpot.core.model.factory.DatasetFactory;

/**
 *
 * @author Charalampos Chomenidis
 * @author Pantelis Sopasakis
 */
@Stateless
public class DatasetHandler extends AbstractHandler<Dataset> {

    @Inject
    @MongoDB
    JaqpotEntityManager em;

    public DatasetHandler() {
        super(Dataset.class);
    }

    @Override
    public void create(Dataset entity) {
        entity.setTotalRows(entity.getDataEntry().size());
        entity.setTotalColumns(entity.getDataEntry()
                .stream()
                .max((e1, e2) -> Integer.compare(e1.getValues().size(), e2.getValues().size()))
                .orElseGet(() -> {
                    DataEntry de = new DataEntry();
                    de.setValues(new TreeMap<>());
                    return de;
                })
                .getValues().size());
        getEntityManager().persist(entity);
    }


    @Override
    protected JaqpotEntityManager getEntityManager() {
        return em;
    }

    public Dataset find(Object id, Integer rowStart, Integer rowMax, Integer colStart, Integer colMax, String stratify, Long seed, Integer folds, String targetFeature) {
        Dataset dataset = em.find(Dataset.class, id);
        if (dataset == null) {
            return null;
        }
        if (stratify != null) {
            switch (stratify) {
                case "random":
                    dataset = DatasetFactory.randomize(dataset, seed);
                    break;
                case "normal":
                    dataset = DatasetFactory.stratify(dataset, folds, targetFeature);
                    break;
                case "default":
                    break;
            }
        }

        if (rowStart == null) {
            rowStart = 0;
        }
        if (colStart == null) {
            colStart = 0;
        }
        dataset.setTotalRows(dataset.getDataEntry().size());
        dataset.setTotalColumns(dataset.getDataEntry()
                .stream()
                .max((e1, e2) -> Integer.compare(e1.getValues().size(), e2.getValues().size()))
                .orElseGet(() -> {
                    DataEntry de = new DataEntry();
                    de.setValues(new TreeMap<>());
                    return de;
                })
                .getValues().size());

        if (rowMax == null || rowMax > dataset.getTotalRows()) {
            rowMax = dataset.getTotalRows();
        }
        if (colMax == null || colMax > dataset.getTotalColumns()) {
            colMax = dataset.getTotalColumns();
        }

        dataset.setDataEntry(dataset.getDataEntry().subList(rowStart, rowStart + rowMax));

        for (DataEntry de : dataset.getDataEntry()) {
            Integer entryColMax = de.getValues().size() < colMax ? de.getValues().size() : colMax;
            TreeMap<String, Object> values = (TreeMap) de.getValues();
            NavigableSet<String> valuesSet = values.navigableKeySet();

            Iterator<String> it = valuesSet.iterator();
            for (int i = 0; i < colStart; i++) {
                it.next();
                it.remove();
            }
            for (int i = 0; i < entryColMax; i++) {
                it.next();
            }
            while (it.hasNext()) {
                it.next();
                it.remove();
            }

        }

//        DataEntry blank = new DataEntry();
//        blank.setValues(new TreeMap<>());
//        DataEntry firstEntry = dataset.getDataEntry().stream().findFirst().orElse(blank);
//        dataset.getFeatures().keySet().retainAll(firstEntry.getValues().keySet());                
        return dataset;
    }

}

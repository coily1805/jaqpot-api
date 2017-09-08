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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.jaqpot.core.annotations.MongoDB;
import org.jaqpot.core.db.entitymanager.JaqpotEntityManager;
import org.jaqpot.core.model.Algorithm;
import org.jaqpot.core.model.Parameter;

/**
 *
 * @author Charalampos Chomenidis
 * @author Pantelis Sopasakis
 */
@Stateless
public class AlgorithmHandler extends AbstractHandler<Algorithm> {

    @Inject
    @MongoDB
    JaqpotEntityManager em;

    public AlgorithmHandler() {
        super(Algorithm.class);
    }

    @Override
    protected JaqpotEntityManager getEntityManager() {
        return em;
    }

    public List<Algorithm> findByOntologicalClass(String className, Integer start, Integer max) {
        Map<String, Object> properties = new HashMap<>();
        List<String> classes = new ArrayList<>();
        classes.add(className);
        properties.put("ontologicalClasses", classes);

        List<String> fields = new ArrayList<>();
        fields.add("_id");
        fields.add("meta");
        fields.add("ontologicalClasses");

        return em.find(Algorithm.class, properties, fields, start, max);
    }

    public Long countByOntologicalClass(String className) {
        Map<String, Object> properties = new HashMap<>();
        List<String> classes = new ArrayList<>();
        classes.add(className);
        properties.put("ontologicalClasses", classes);
        return em.count(Algorithm.class, properties);
    }

    public Algorithm findAndAddParameterType(Object primaryKey) {
        Algorithm algo = em.find(Algorithm.class, primaryKey);
        Optional<Set<Parameter>> parameters = Optional.of(algo).map(Algorithm::getParameters);
        parameters.ifPresent(ParameterSet -> {
            Set<Parameter> paramUpdates = new HashSet<>();
            ParameterSet.forEach((p) -> {
                if (p.getValueType() == null) {
                    p.setValueType(Parameter.ValueType.VALID_JSON_DESCRIBED);
                    if (p.getValue().getClass().getSimpleName().equals("ArrayList")) {
                        ArrayList<Object> check = new ArrayList();
                        check.add(p.getValue());
                        ArrayList<Object> value = (ArrayList) check.get(0);
                        String valueType = value.get(0).getClass().getSimpleName();
                        switch (valueType) {
                            case ("Integer"):
                                p.setValueType(Parameter.ValueType.INTEGER_ARRAY);
                                break;
                            case ("Double"):
                                p.setValueType(Parameter.ValueType.DOUBLE_ARRAY);
                                break;
                            case ("String"):
                                p.setValueType(Parameter.ValueType.STRING_ARRAY);
                                break;
                        }
//                    p.setValueTypeString("ArrayList<" + valueType + ">");
                        paramUpdates.add(p);
                    } else {
                        String valueType = p.getValue().getClass().getSimpleName();
                        switch (valueType) {
                            case ("Integer"):
                                p.setValueType(Parameter.ValueType.INTEGER);
                                break;
                            case ("Double"):
                                p.setValueType(Parameter.ValueType.DOUBLE);
                                break;
                            case ("String"):
                                p.setValueType(Parameter.ValueType.STRING);
                        }
//                    p.setValueTypeString(p.getValue().getClass().getSimpleName());
                        paramUpdates.add(p);
                    }
                }
            });
            algo.getParameters().clear();
            algo.setParameters(paramUpdates);
        });
        return algo;
    }
}

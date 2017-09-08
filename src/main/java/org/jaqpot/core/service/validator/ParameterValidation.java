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
package org.jaqpot.core.service.validator;

import static java.lang.Boolean.FALSE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.jaqpot.core.annotations.Jackson;
import org.jaqpot.core.data.serialize.JSONSerializer;
import org.jaqpot.core.data.serialize.JaqpotSerializationException;
import org.jaqpot.core.model.Parameter;
import org.jaqpot.core.model.Parameter.ValueType;
import org.jaqpot.core.service.exceptions.parameter.ParameterFormationException;
import org.jaqpot.core.service.exceptions.parameter.ParameterRangeException;
import org.jaqpot.core.service.exceptions.parameter.ParameterScopeException;
import org.jaqpot.core.service.exceptions.parameter.ParameterTypeException;

/**
 *
 * @author pantelispanka
 */
@Stateless
public class ParameterValidation {

    private JSONSerializer serializer;

    public ParameterValidation() {
    }

    @Inject
    public ParameterValidation(@Jackson JSONSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Checks the formation of the parameter String. It's validation depends
     * from the ability of the Jackson Serializer to deserialize the Json String
     *
     * @param input
     *
     * @return
     * @throws
     * org.jaqpot.core.service.exceptions.parameter.ParameterFormationException
     */
    public Map<String, Object> checkFormation(String input) throws ParameterFormationException {
        Map<String, Object> parameterMap = null;
        try {
            parameterMap = serializer.parse(input, new HashMap<String, Object>().getClass());
        } catch (JaqpotSerializationException ex) {
            throw new ParameterFormationException(ex.getMessage());
        }
        return parameterMap;
    }

    /**
     *
     * Checks the existence of mandatory parameters in the input string of
     * parameters
     *
     *
     * @param parameterMap
     * @param parameters
     *
     * @throws
     * org.jaqpot.core.service.exceptions.parameter.ParameterScopeException
     */
    public void checkParameterExistence(Map<String, Object> parameterMap, Set<Parameter> parameters) throws ParameterScopeException {
        for (Parameter parameter : parameters) {
            if (parameter.getScope().equals(Parameter.Scope.MANDATORY) && (parameterMap == null || !parameterMap.containsKey(parameter.getId()))) {
                throw new ParameterScopeException("Parameter with id: '" + parameter.getId() + "' is mandatory.");
            }
        }
    }

    /**
     * Checks if the parameter id is given right
     *
     *
     * @param entry
     * @param parameters
     * @return
     * @throws ParameterTypeException
     */
    public Parameter checkParamatersId(Map.Entry<String, Object> entry,
            Set<Parameter> parameters) throws ParameterTypeException {
        String parameterId = entry.getKey();
        Parameter parameter = parameters.stream()
                .filter(p -> p.getId().equals(parameterId))
                .findFirst().orElseThrow(
                        ()
                        -> new ParameterTypeException("Could not recognise parameter with id:"
                                + parameterId));
        return parameter;
    }

    /**
     * Checks if the parameter type is given in the right form against the Enum
     * ValueType from database
     *
     * @param entry
     * @param parameter
     * @return
     * @throws ParameterTypeException
     */
    public ValueType checkParameterType(Map.Entry<String, Object> entry,
            Parameter parameter) throws ParameterTypeException {

        Parameter.ValueType parameterFromUser = Parameter.ValueType.VALID_JSON_DESCRIBED;
        if (entry.getValue().getClass().getSimpleName().equals("ArrayList")) {
            ArrayList<Object> valueToEnum = (ArrayList) entry.getValue();
            String switchingValue = valueToEnum.get(0).getClass().getSimpleName();
            switch (switchingValue) {
                case ("Integer"):
                    Integer intVal = (Integer) valueToEnum.get(0);
                    if (parameter.getValueType().equals(Parameter.ValueType.DOUBLE_ARRAY)) {
                        try {
                            Double.parseDouble(""+valueToEnum.get(0));
                            parameterFromUser = Parameter.ValueType.DOUBLE_ARRAY;
                            break;
                        } catch (ClassCastException e) {
                            throw new ParameterTypeException("Parameter with id "
                                    + parameter.getId() + " is not given in the right type."
                                    + " Should be " + parameter.getValueType().toString()
                                    + " and it is given in " + ValueType.INTEGER_ARRAY);
                        }
                    }
                    parameterFromUser = Parameter.ValueType.INTEGER_ARRAY;
                    break;
                case ("Double"):
                    if (parameter.getValueType().equals(Parameter.ValueType.INTEGER_ARRAY)) {
                        Double check = (Double) valueToEnum.get(0);
                        
                        Double val = check - check.intValue();
                        if (Math.abs(val) < 2 * Double.MIN_VALUE) {
                            parameterFromUser = Parameter.ValueType.INTEGER_ARRAY;
                        }else{
                            parameterFromUser = Parameter.ValueType.DOUBLE_ARRAY;
                        }
                        break;
                    }
                    parameterFromUser = Parameter.ValueType.DOUBLE_ARRAY;
                    break;
                case ("String"):
                    parameterFromUser = Parameter.ValueType.STRING_ARRAY;
                    break;
            }
        } else {
            Object valueToEnum = entry.getValue();
            switch (valueToEnum.getClass().getSimpleName()) {
                case ("Integer"):
                    if (parameter.getValueType().equals(Parameter.ValueType.DOUBLE)) {
                        try {
                            
//                            Double check = (Double) valueToEnum;
                            Double.parseDouble(""+valueToEnum);
                            parameterFromUser = Parameter.ValueType.DOUBLE;
                            break;
                        } catch (ClassCastException e) {
                            throw new ParameterTypeException("Parameter with id "
                                    + parameter.getId() + " is not given in the right type."
                                    + " Should be " + parameter.getValueType().toString()
                                    + " and it is given in " + ValueType.INTEGER);
                        }
                    }
                    parameterFromUser = Parameter.ValueType.INTEGER;
                    break;
                case ("Double"):
                    if (parameter.getValueType().equals(Parameter.ValueType.INTEGER)){
                        Double check = (Double) entry.getValue();
                        Double val = check - check.intValue();
                        if (Math.abs(val) < 2 * Double.MIN_VALUE) {
                            parameterFromUser = Parameter.ValueType.INTEGER;
                        }else{
                            parameterFromUser = Parameter.ValueType.DOUBLE_ARRAY;
                        }
                        break;
                    }
                    parameterFromUser = Parameter.ValueType.DOUBLE;
                    break;
                case ("String"):
                    parameterFromUser = Parameter.ValueType.STRING;
                    break;
            }
        }

        if (parameterFromUser != Parameter.ValueType.VALID_JSON_DESCRIBED && parameterFromUser != parameter.getValueType()) {
            throw new ParameterTypeException("Parameter with id "
                    + parameter.getId() + " is not given in the right type."
                    + " Should be " + parameter.getValueType().toString()
                    + " and it is given in " + parameterFromUser);
        }

        return parameterFromUser;
    }

    /**
     *
     * @param entry
     * @param parameter
     * @throws ParameterRangeException
     */
    public void checkParameterAllowedValues(Map.Entry<String, Object> entry,
            Parameter parameter) throws ParameterRangeException {

        if (parameter.getAllowedValues() != null) {
            if (!parameter.getAllowedValues().contains(entry.getValue())) {
                throw new ParameterRangeException("Parameter with id: '"
                        + parameter.getId()
                        + "' has a value not found in allowed values.");
            }
        }
    }

    /**
     *
     * @param entry
     * @param parameter
     * @throws ParameterRangeException
     */
    public void checkMinValue(Map.Entry<String, Object> entry, Parameter parameter) throws ParameterRangeException {

        if (parameter.getMinValue() != null && isNumeric(parameter.getMinValue().toString())) {

            if (entry.getValue().getClass().getSimpleName().equals("ArrayList")) {

                List<Object> listValue = (ArrayList<Object>) entry.getValue();

                checkIsLessThan(parameter.getId(),
                        Double.parseDouble(listValue.get(0).toString()),
                        (Double.parseDouble(parameter.getMinValue().toString())));

            } else {

                checkIsLessThan(parameter.getId(),
                        Double.parseDouble(entry.getValue().toString()),
                        (Double.parseDouble(parameter.getMinValue().toString())));

            }
        }

    }

    /**
     *
     * @param entry
     * @param parameter
     * @throws ParameterRangeException
     */
    public void checkMaxValue(Map.Entry<String, Object> entry, Parameter parameter) throws ParameterRangeException {
        if (parameter.getMinValue() != null && isNumeric(parameter.getMaxValue().toString())) {

            if (entry.getValue().getClass().getSimpleName().equals("ArrayList")) {

                List<Object> listValue = (ArrayList<Object>) entry.getValue();

                checkIsGreaterThan(parameter.getId(),
                        Double.parseDouble(listValue.get(0).toString()),
                        (Double.parseDouble(parameter.getMinValue().toString())));
            } else {

                checkIsGreaterThan(parameter.getId(),
                        Double.parseDouble(entry.getValue().toString()),
                        (Double.parseDouble(parameter.getMinValue().toString())));

            }
        }

    }

    /**
     *
     * @param <T>
     * @param parameterId
     * @param value
     * @param maximum
     *
     * @throws ParameterRangeException
     */
    private static <T extends Comparable<? super T>> void checkIsGreaterThan(String parameterId, T value, T maximum) throws ParameterRangeException {
        if (maximum != null && isNumeric(maximum.toString())) {
            if (value.compareTo(maximum) > 0) {
                throw new ParameterRangeException("Parameter with id: '" + parameterId + "' has a value greater than the parameter's allowed maximum");
            }
        }

    }

    /**
     *
     * @param <T>
     * @param parameterId
     * @param value
     * @param minimum
     *
     * @throws ParameterRangeException
     */
    private static <T extends Comparable<? super T>> void checkIsLessThan(String parameterId, T value, T minimum) throws ParameterRangeException {
        if (minimum != null && isNumeric(minimum.toString())) {
            if (value.compareTo(minimum) < 0) {
                throw new ParameterRangeException("Parameter with id: '" + parameterId + "' has a value less than the parameter's allowed minimum");
            }
        }
    }

    private static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}

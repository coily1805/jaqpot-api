/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jaqpot.core.service.data;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import org.jaqpot.core.data.serialize.JacksonJSONSerializer;
import org.jaqpot.core.service.client.Util;
import org.jaqpot.core.service.dto.bundle.BundleSubstances;
import org.jaqpot.core.service.dto.dataset.DataEntry;
import org.jaqpot.core.service.dto.dataset.Dataset;
import org.jaqpot.core.service.dto.dataset.Substance;
import org.jaqpot.core.service.dto.study.Effect;
import org.jaqpot.core.service.dto.study.Studies;
import org.jaqpot.core.service.dto.study.Study;

/**
 *
 * @author hampos
 */
@Stateless
public class ConjoinerService {

    public Dataset prepareDataset(String bundleURI, String subjectId) {

        try {
            Client client = Util.buildUnsecureRestClient();

            BundleSubstances substances = client.target(bundleURI + "/substance")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get(BundleSubstances.class);
            Dataset dataset = new Dataset();
            List<DataEntry> dataEntries = new ArrayList<>();

            for (Substance s : substances.getSubstance()) {
                Studies studies = client.target(s.getURI() + "/study")
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get(Studies.class);
                DataEntry dataEntry = createDataEntry(studies);
                dataEntries.add(dataEntry);
            }

            dataset.setDatasetURI(UUID.randomUUID().toString());
            dataset.setDataEntry(dataEntries);

            return dataset;
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(ConjoinerService.class.getName()).log(Level.SEVERE, null, ex);
            throw new InternalServerErrorException(ex);
        }
    }

    public DataEntry createDataEntry(Studies studies) {
        DataEntry dataEntry = new DataEntry();
        Substance compound = new Substance();
        Map<String, Object> values = new HashMap<>();
        for (Study study : studies.getStudy()) {
            compound.setURI(study.getOwner().getSubstance().getUuid());
            for (Effect effect : study.getEffects()) {
                JacksonJSONSerializer serializer = new JacksonJSONSerializer();
                String name = effect.getEndpoint();
                String units = effect.getResult().getUnit();
                String conditions = serializer.write(effect.getConditions());
                String identifier = createHashedIdentifier(name, units, conditions);
                String topcategory = study.getProtocol().getTopcategory();
                String endpointcategory = study.getProtocol().getCategory().getCode();
                List<String> guidelines = study.getProtocol().getGuideline();
                String guideline = guidelines == null || guidelines.isEmpty() ? "" : guidelines.get(0);
                String propertyURI = getRelativeURI(name, topcategory, endpointcategory, identifier, Boolean.TRUE, guideline);
                Object value = calculateValue(effect);
                if (value == null) {
                    continue;
                }
                values.put(propertyURI, value);
            }
        }
        dataEntry.setCompound(compound);
        dataEntry.setValues(values);

        return dataEntry;
    }

    public Object calculateValue(Effect effect) {
        return effect.getResult().getLoValue() == null ? null : effect.getResult().getLoValue();
    }

    public String getRelativeURI(String name, String topcategory, String endpointcategory, String identifier, Boolean extendedURI, String guideline) {
        try {
            return String.format("/property/%s/%s%s%s/%s%s%s",
                    URLEncoder.encode(topcategory == null ? "TOX" : topcategory, "UTF-8"), URLEncoder.encode(endpointcategory == null ? "UNKNOWN_TOXICITY_SECTION" : endpointcategory, "UTF-8"), name == null ? "" : "/", name == null ? "" : URLEncoder.encode(name, "UTF-8"), identifier, extendedURI ? "/" : "", extendedURI ? URLEncoder.encode(UUID.nameUUIDFromBytes(guideline.getBytes()).toString(), "UTF-8") : "");
        } catch (UnsupportedEncodingException x) {
            return "/property";
        }
    }

    public String createHashedIdentifier(String name, String units, String conditions) {
        HashFunction hf = Hashing.sha1();
        StringBuilder b = new StringBuilder();
        b.append(name == null ? "" : name);
        b.append(units == null ? "" : units);
        b.append(conditions == null ? "" : conditions);

        HashCode hc = hf.newHasher()
                .putString(b.toString(), Charsets.US_ASCII)
                .hash();
        return hc.toString().toUpperCase();
    }

}
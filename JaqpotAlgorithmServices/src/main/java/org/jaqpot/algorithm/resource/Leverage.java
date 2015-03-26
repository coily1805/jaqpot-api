/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jaqpot.algorithm.resource;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jaqpot.algorithm.model.LeverageModel;
import org.jaqpot.core.model.dto.dataset.Dataset;
import org.jaqpot.core.model.dto.jpdi.PredictionRequest;
import org.jaqpot.core.model.dto.jpdi.PredictionResponse;
import org.jaqpot.core.model.dto.jpdi.TrainingRequest;
import org.jaqpot.core.model.dto.jpdi.TrainingResponse;
import org.jaqpot.core.model.factory.ErrorReportFactory;

/**
 *
 * @author hampos
 */
@Path("leverage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Leverage {

    @POST
    @Path("training")
    public Response training(TrainingRequest request) {
        try {
            Dataset dataset = request.getDataset();

            int numOfSubstances = dataset.getDataEntry().size();
            int numOfFeatures = dataset.getDataEntry().stream().findFirst().get().getValues().size();

            double[][] dataArray = new double[numOfSubstances][numOfFeatures];

            for (int i = 0; i < numOfSubstances; i++) {
                dataArray[i] = dataset.getDataEntry()
                        .get(i)
                        .getValues()
                        .entrySet()
                        .stream()
                        .mapToDouble(entry -> {
                            return Double.parseDouble(entry.getValue().toString());
                        }).toArray();
            }
            Matrix dataMatrix = new Matrix(dataArray);
            double[][] omega = (dataMatrix.transpose().times(dataMatrix)).getArray();
            double gamma = (3.0 * numOfFeatures) / numOfSubstances;

            LeverageModel model = new LeverageModel();
            model.setOmega(omega);
            model.setGamma(gamma);

            TrainingResponse response = new TrainingResponse();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(model);
            String base64Model = Base64.getEncoder().encodeToString(baos.toByteArray());
            response.setRawModel(base64Model);
            response.setIndependentFeatures(new ArrayList<>(dataset.getDataEntry().get(0).getValues().keySet()));
            return Response.ok(response).build();
        } catch (IOException ex) {
            Logger.getLogger(Leverage.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ErrorReportFactory.internalServerError()).build();
        }
    }

    @POST
    @Path("prediction")
    public Response prediction(PredictionRequest request) {
        try {
            String base64Model = (String) request.getRawModel();
            byte[] modelBytes = Base64.getDecoder().decode(base64Model);
            ByteArrayInputStream bais = new ByteArrayInputStream(modelBytes);
            ObjectInput in = new ObjectInputStream(bais);
            LeverageModel model = (LeverageModel) in.readObject();

            Dataset dataset = request.getDataset();

            int numOfSubstances = dataset.getDataEntry().size();
            int numOfFeatures = dataset.getDataEntry().stream().findFirst().get().getValues().size();

            double[][] dataArray = new double[numOfSubstances][numOfFeatures];

            for (int i = 0; i < numOfSubstances; i++) {
                dataArray[i] = dataset.getDataEntry()
                        .get(i)
                        .getValues()
                        .entrySet()
                        .stream()
                        .mapToDouble(entry -> {
                            return Double.parseDouble(entry.getValue().toString());
                        }).toArray();
            }
            Matrix dataMatrix = new Matrix(dataArray);
            Matrix omega = new Matrix(model.getOmega());
            omega.print(10, 2);
            double gamma = model.getGamma();
            Matrix x = null;
            List<Object> predictions = new ArrayList<>();
            SingularValueDecomposition svd = omega.svd();
            Matrix S = svd.getS();
            Matrix U = svd.getU();
            Matrix V = svd.getV();

            for (int i = 0; i < S.getRowDimension(); i++) {
                if (Math.abs(S.get(i, i)) > 1e-6) {
                    S.set(i, i, 1 / S.get(i, i));
                }
            }
            Matrix pseudoInverse = U.times(S).times(V.transpose());
            pseudoInverse.print(10, 2);
            for (int i = 0; i < numOfSubstances; i++) {
                x = dataMatrix.getMatrix(i, i, 0, numOfFeatures - 1);
                x.print(10, 2);

                double indicator = Math.max(0, (gamma - x.times(pseudoInverse.times(x.transpose())).get(0, 0)) / gamma);
                predictions.add(indicator);
            }
            PredictionResponse response = new PredictionResponse();
            response.setPredictions(predictions);
            return Response.ok(response).build();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Leverage.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorReportFactory.internalServerError())
                    .build();
        }
    }

}
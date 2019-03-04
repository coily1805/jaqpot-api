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
package org.jaqpot.core.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jaqpot.core.messagebeans.IndexEntityProducer;
import org.jaqpot.core.model.ErrorReport;
import org.jaqpot.core.model.JaqpotEntity;
import org.jaqpot.core.search.engine.JaqpotSearch;
import org.jaqpot.core.service.annotations.TokenSecured;
import org.jaqpot.core.service.authentication.RoleEnum;

/**
 *
 * @author pantelispanka
 */
@Path("search")
@Api(value = "/search", description = "Search API")
@Produces({"application/json", "text/uri-list"})
public class SearchResource {

    @EJB
    JaqpotSearch jaqpotSearch;
    
    @Inject
    IndexEntityProducer imp;

    @GET
    @TokenSecured({RoleEnum.DEFAULT_USER})
    @Produces({MediaType.APPLICATION_JSON, "text/uri-list"})
    @ApiOperation(
            value = "Searches for Jaqpot Entities",
            notes = "Searches for Jaqpot Entities"
    )
    @ApiResponses(value = {
        @ApiResponse(code = 401, response = ErrorReport.class, message = "Wrong, missing or insufficient credentials. Error report is produced.")
        ,
            @ApiResponse(code = 200, response = JaqpotEntity.class, responseContainer = "List", message = "A list of jaqpot entities found")
        ,
            @ApiResponse(code = 500, response = ErrorReport.class, message = "Internal server error - this request cannot be served.")

    })
    public Response search(
            @QueryParam("term") String term
    ) {
        int from = 0;
        int to = 40;
        jaqpotSearch.term(term, from, to);
        return Response
                .ok()
                .build();
    }

}

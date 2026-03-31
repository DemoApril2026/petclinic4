package org.springframework.samples.petclinic.pet;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * Reactive CRUD operations for Pet Types using Spring WebFlux
 */
@RestController
@RequestMapping("/petclinic/api/pettypes")
@CrossOrigin(
 methods = {PUT, POST, GET, OPTIONS, DELETE, PATCH},
 maxAge = 3600,
 allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"},
 origins = "*"
)
@Tag(name = "Pet Types Api")
public class PetTypeReactiveController {
    
    /** Inject service implementation layer. */
    private PetReactiveServices petServices;
    
    /** Injection with controller. */
    public PetTypeReactiveController(PetReactiveServices petServices) {
        this.petServices = petServices;
    }
    
    /**
     * List all pet types from reference tables.
     *
     * @return
     *      A set of all pets
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Read all pet types from database")
    @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of pet types"), 
      @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Set<PetType>>> getAllPetTypes() {
        return petServices.findAllPetTypes().map(ResponseEntity::ok);
    }
    
    @GetMapping(value = "/{petTypeId}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve pet information information by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "the identifier exists and related pet type is returned"), 
        @ApiResponse(responseCode = "400", description = "The name was not valid"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })        
    public Mono<ResponseEntity<PetType>> getType(
            @PathVariable("petTypeId") 
            @Parameter(required = true,example = "surgery",
            description = "Unique identifier of a Pet Type") String name) {
        return petServices.listPetType()
                          .map(set -> (set.contains(name) ? 
                        new ResponseEntity<PetType>(new PetType(name), HttpStatus.OK) :
                        new ResponseEntity<PetType>(HttpStatus.NOT_FOUND)));
    }
    
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PetType>> addType(@RequestBody PetType petType) {
        return petServices.addPetType(petType).map(ResponseEntity::ok);
    }
    
    @PutMapping(value = "/{petTypeId}", produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PetType>> updatePetType(
            @PathVariable("petTypeId") String name, 
            @RequestBody PetType petType) {
        petType.setId(petType.getName());
        return petServices.replacePetType(name, petType.getName())
                          .map(ResponseEntity::ok);
    }
    
    @DeleteMapping(value = "/{petTypeId}", produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> deletePetType(@PathVariable("petTypeId") String name){
        return petServices.removePetType(name)
                          .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
    }

}

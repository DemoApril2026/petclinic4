package org.springframework.samples.petclinic.pet;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive CRUD operations for Pets using Spring WebFlux
 */
@RestController
@Validated
@RequestMapping("/petclinic/api/pets")
@CrossOrigin(
 methods = {PUT, POST, GET, OPTIONS, DELETE, PATCH},
 maxAge = 3600,
 allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"},
 origins = "*"
)
@Tag(name = "Pets Api")
public class PetReactiveController {
    
    /** Inject service implementation layer. */
    private PetReactiveServices petServices;
    
    /** Injection with controller. */
    public PetReactiveController(PetReactiveServices petServices) {
        this.petServices = petServices;
    }
    
    /**
     * Read all pets from database.
     *
     * @return
     *   a {@link Flux} containing {@link PetEntity}
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Read all pets in database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of pets (even if empty)"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Flux<Pet> findAllPets() {
        return petServices.findAllPets();
    }

    /**
     * Retrieve a pet's information by its unique identifier.
     *
     * @param ownerId
     *      unique identifer as a String, to be converted in {@link UUID}.
     * @return
     *      a {@link Mono} of {@link OwnerEntity} or empty response with not found (404) code
     */
    @GetMapping(value = "/{petId}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve pet information by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "the identifier exists and related pet is returned"), 
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"), 
        @ApiResponse(responseCode = "404", description = "the identifier does not exist in DB"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Pet>> findPetById(@PathVariable("petId") @Parameter(
               required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
               description = "Unique identifier of a Pet") @NotBlank String petId) {
        return petServices.findPetByPetId(UUID.fromString(petId))
                          .map(ResponseEntity::ok)
                          .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Create a {@link PetEntity} when we don't know the identifier.
     *
     * @param request
     *      current http request
     * @param vetRequest
     *      fields required to create pet (no uid)
     * @return
     *      the created owner.
     */
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new Pet, an unique identifier is generated and returned")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The pet has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "Invalid Dto provided"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Pet>> createPet(
            UriComponentsBuilder uc, 
            @RequestBody @Valid Pet pet) {
        pet.setId(UUID.randomUUID());
        return petServices.createPet(pet)
                          .map(created -> mapPetAsHttpResponse(uc, created));
    }
    
    /**
     * Create or update a {@link PetEntity}. We do not throw an exception if the entity already exists
     * or check existence, as this would require a read before write, and Cassandra supports an
     * upsert style of interaction.
     *
     * @param petId
     *      unique identifier for pet
     * @param pet
     *      pet as Pet
     * @return
     *      the created Pet.
     */
    @PutMapping(value="/{petId}",  
                consumes=APPLICATION_JSON_VALUE,
                produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert a pet (no read before write as for Cassandra)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The pet has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "The pet bean was malformed or does not provide valid id"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Pet>> upsertPet(
            UriComponentsBuilder uc, 
            @PathVariable("petId") @NotBlank String petId, 
            @RequestBody @Valid Pet pet) {
      Assert.isTrue(UUID.fromString(petId).equals(pet.getId()), 
              "Pet identifier provided in pet does not match the value if path");
      return petServices.createPet(pet)
                        .map(created -> mapPetAsHttpResponse(uc, created));
    }
    
    /**
     * Delete a pet by its unique identifier.
     *
     * @param petId
     *      pet identifier
     * @return
     */
    @DeleteMapping("/{petId}")
    @Operation(summary = "Delete a pet by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The pet has been deleted"), 
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Void>> deleteById(
            @NotBlank 
            @PathVariable("petId") 
            @Parameter(required = true,
                       example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
                       description = "Unique identifier of a pet") 
            String petId) {
        return petServices.deletePetById(UUID.fromString(petId))
                     .map(v -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT));
    }
    
    /**
     * Syntaxic sugar to create response from call.
     */
    protected ResponseEntity<Pet> mapPetAsHttpResponse(UriComponentsBuilder ucBuilder, Pet created) {
        return ResponseEntity.created(ucBuilder.path("/api/pets/{id}")
                        .buildAndExpand(created.getId().toString())
                        .toUri()).body(created);
    }
    
}

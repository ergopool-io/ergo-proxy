package proxy

import io.swagger.v3.oas.models.media.{ArraySchema, Content, MediaType, Schema}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem, Paths}
import scala.jdk.CollectionConverters._

object ProxySwagger {
  /**
   * Get proxy swagger openAPI
   * @param openAPI the open api to add proxy specifications to it
   * @return
   */
  def getProxyOpenAPI(openAPI: OpenAPI): OpenAPI = {
    openAPI.getComponents.getSchemas.put("ProxySuccess", proxySuccessSchema)
    addPB(openAPI)

    addShareEndpoint(openAPI)

    addProxyEndpoints(openAPI)

    openAPI
  }

  /**
   * Success schema
   *
   * @return
   */
  private def successSchema: Schema[_] = {
    val successSchema: Schema[_] = new Schema()
    successSchema.setType("boolean")
    successSchema.setDescription("True if operation was successful")
    successSchema.setExample(true)

    successSchema
  }

  /**
   * Proxy success schema
   * @return
   */
  private def proxySuccessSchema: Schema[_] = {
    val schema: Schema[_] = new Schema()

    schema.setRequired(List[String]("success").asJava)
    schema.setType("object")

    schema.setProperties(Map[String, Schema[_]]("success" -> successSchema).asJava)

    schema
  }

  /**
   * Add pb to /mining/candidate
   * @param openAPI the object to add pb
   */
  private def addPB(openAPI: OpenAPI): Unit = {
    val pbSchema = new Schema()
    pbSchema.setType("Integer")
    pbSchema.setExample(9876543210L)
    openAPI.getComponents.getSchemas.get("ExternalCandidateBlock").addProperties("pb", pbSchema)
  }

  /**
   * Add share endpoint to swagger
   * @param openAPI the object to add the endpoint
   */
  private def addShareEndpoint(openAPI: OpenAPI): Unit = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is valid")
      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is invalid")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ApiError")

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      response500.setDescription("Error")
      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val security = new SecurityRequirement
      security.addList("ApiKeyAuth", "[api_key]")

      val op = new Operation
      op.addSecurityItem(security)
      op.setSummary("Submit share for current candidate")
      op.addTagsItem("mining")
      op.setRequestBody(openAPI.getPaths.get("/mining/solution").getPost.getRequestBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(openAPI, "/mining/share", path, "/mining/rewardAddress")
  }

  /**
   * Add pathItem after "after" key
   *
   * @param openAPI the object to add pathItem to
   * @param pathName name of the path
   * @param pathItem the pathItem to add
   * @param after the path that pathItem should be put after it
   */
  private def addPath(openAPI: OpenAPI, pathName: String, pathItem: PathItem, after: String): Unit = {
    // Reformat paths of openAPI
    val newPaths = new Paths
    openAPI.getPaths.forEach({
      case (name, value) =>
        newPaths.addPathItem(name, value)
        if (name == after)
          newPaths.addPathItem(pathName, pathItem)
    })
    openAPI.setPaths(newPaths)
  }

  /**
   * Add proxy endpoints to the swagger
   *
   * @param openAPI the openApi to use
   */
  private def addProxyEndpoints(openAPI: OpenAPI): Unit = {
    addConfigReload(openAPI)

    addStatusReset(openAPI)

    addTest(openAPI)
  }

  /**
   * Add /proxy/config/reload to swagger
   *
   * @param openAPI the openApi to use
   */
  private def addConfigReload(openAPI: OpenAPI): Unit = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Config reloaded")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 response
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.setDefault(response200)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val op = new Operation
      op.setSummary("Reload proxy config from the pool server")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(openAPI, "/proxy/config/reload", path, "/info")
  }

  /**
   * Add /proxy/status/reset to swagger
   *
   * @param openAPI the openApi to use
   */
  private def addStatusReset(openAPI: OpenAPI): Unit = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Status reset successfully")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Reset status failed")

      val schema: Schema[_] = new Schema()

      schema.setRequired(List[String]("success", "message").asJava)
      schema.setType("object")

      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: Schema[_] = new Schema()
      messageSchema.setType("string")
      messageSchema.setDescription("reason of failure in operation")
      messageSchema.setExample("Something happened")

      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val op = new Operation
      op.setSummary("Reset status of proxy")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(openAPI, "/proxy/status/reset", path, "/info")
  }

  /**
   * Add /proxy/test to swagger
   *
   * @param openAPI the openApi to use
   */
  private def addTest(openAPI: OpenAPI): Unit = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Proxy is working")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ProxySuccess")

      response.setContent(jsonContentType(schema))

      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Exception happened when testing proxy")

      val schema: Schema[_] = new Schema()

      schema.setRequired(List[String]("success", "messages").asJava)
      schema.setType("object")

      val failSchema: Schema[_] = successSchema
      failSchema.setExample(false)

      val messageSchema: ArraySchema = new ArraySchema()
      messageSchema.setType("array")
      messageSchema.setDescription("List of reasons of failure")
      messageSchema.getItems

      messageSchema.setItems({
        val s: Schema[_] = new Schema()
        s.setType("string")
        s.setDescription("error messages during the test")
        s
      })
      schema.setProperties(Map[String, Schema[_]]("success" -> failSchema, "message" -> messageSchema).asJava)

      response.setContent(jsonContentType(schema))

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      responses.setDefault(response500)

      responses
    }

    // Create a get operation
    val getOperation: Operation = {
      val op = new Operation
      op.setSummary("Test proxy is working")
      op.addTagsItem("proxy")
      op.setResponses(APIResponses)

      op
    }

    // Add get Operation to paths
    val path: PathItem = new PathItem
    path.setGet(getOperation)

    addPath(openAPI, "/proxy/test", path, "/info")
  }

  /**
   * Return a json content type
   *
   * @param schema schema to use for content
   * @return
   */
  private def jsonContentType(schema: Schema[_]): Content = {
    val mediaType: MediaType = new MediaType
    mediaType.setSchema(schema)

    val content: Content = new Content
    content.addMediaType("application/json", mediaType)
  }
}

package net.gvmtool.release.defaults

import net.gvmtool.release.candidate.{Candidate, CandidateGeneralRepo, CandidateUpdateRepo}
import net.gvmtool.release.request.DefaultVersionRequest
import net.gvmtool.release.response.SuccessResponse
import net.gvmtool.release.version.{Version, VersionRepo}
import org.bson.types.ObjectId
import org.hamcrest.beans.SamePropertyValuesAs._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import org.springframework.http.HttpStatus._
import org.springframework.http.{HttpStatus, ResponseEntity}

class DefaultVersionControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  val mockCandidateUpdateRepo = mock[CandidateUpdateRepo]
  val mockCandidateGenRepo = mock[CandidateGeneralRepo]
  val mockVersionRepo = mock[VersionRepo]

  "default version controller" should {
    "mark an existing candidate version as default" in new ControllerUnderTest {
      //given
      val candidate = "groovy"
      val version = "2.3.6"
      val request = new DefaultVersionRequest(candidate, version)

      val candidateObj = Candidate(candidate, version)
      val persisted = candidateObj.copy(id = new ObjectId("5423333bba78831a730c18e2"))

      val versionFound = Version(
        id = new ObjectId("5426b99bba78e60054fe48ca"), candidate, version,
        url = "http://dl.bintray.com/groovy/maven/groovy-binary-2.3.6.zip")

      when(candidateGenRepo.findByCandidate(candidate)).thenReturn(Candidate(candidate, "2.3.6"))
      when(mockCandidateUpdateRepo.updateDefault(argThat[Candidate](samePropertyValuesAs(candidateObj)))).thenReturn(persisted)
      when(mockVersionRepo.findByCandidateAndVersion(candidate, version)).thenReturn(versionFound)

      //when
      val response: ResponseEntity[SuccessResponse] = default(request)

      //then
      response.getStatusCode shouldBe ACCEPTED
      response.getBody.getId shouldBe "5423333bba78831a730c18e2"
      response.getBody.getMessage shouldBe "default groovy version: 2.3.6"

      verify(mockVersionRepo).findByCandidateAndVersion(candidate, version)
      verify(mockCandidateUpdateRepo).updateDefault(argThat[Candidate](samePropertyValuesAs(persisted)))
    }

    "reject an invalid candidate version as default declaring bad request" in new ControllerUnderTest {
      //given
      val candidate = "groovy"
      val version = "9.9.9"
      val request = new DefaultVersionRequest(candidate, version)

      when(candidateGenRepo.findByCandidate(candidate)).thenReturn(Candidate(candidate, "2.3.6"))
      when(versionRepo.findByCandidateAndVersion(candidate, version)).thenReturn(null)

      //when
      val e = intercept[VersionNotFoundException] {
        default(request)
      }

      //then
      e.getMessage shouldBe "invalid candidate version: groovy 9.9.9"
      verify(versionRepo).findByCandidateAndVersion(candidate, version)
    }

    "reject invalid candidate as bad request when marking default" in new ControllerUnderTest {
      //given
      val candidate = "groovee"
      val version = "2.3.7"
      val request = new DefaultVersionRequest(candidate, version)

      when(candidateGenRepo.findByCandidate(candidate)).thenReturn(null)

      //when
      val e = intercept[CandidateNotFoundException] {
        default(request)
      }

      //then
      e.getMessage shouldBe "not a valid candidate: groovee"
    }

    "handle version not found exceptions with error response" in new ControllerUnderTest {
      val message = "version not found"
      val response = handle(VersionNotFoundException(message))
      response.getStatusCode shouldBe HttpStatus.BAD_REQUEST
      response.getBody.getMessage shouldBe message
    }

    "handle candidate not found exceptions with error response" in new ControllerUnderTest {
      val message = "candidate not found"
      val response = handle(CandidateNotFoundException(message))
      response.getStatusCode shouldBe HttpStatus.BAD_REQUEST
      response.getBody.getMessage shouldBe message
    }
  }

  trait ControllerUnderTest extends DefaultVersionController {
    val candidateUpdateRepo = mockCandidateUpdateRepo
    val versionRepo = mockVersionRepo
    val candidateGenRepo = mockCandidateGenRepo
  }

}

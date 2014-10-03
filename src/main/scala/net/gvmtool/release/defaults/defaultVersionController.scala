package net.gvmtool.release.defaults

import net.gvmtool.release.candidate.{Candidate, CandidateGeneralRepo, CandidateNotFoundException, CandidateUpdateRepo}
import net.gvmtool.release.request.DefaultVersionRequest
import net.gvmtool.release.version.{VersionNotFoundException, VersionRepo}
import net.gvmtool.status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation._

trait DefaultVersionController {

  val candidateUpdateRepo: CandidateUpdateRepo

  val candidateGenRepo: CandidateGeneralRepo

  val versionRepo: VersionRepo

  @RequestMapping(value = Array("/default"), method = Array(PUT))
  def default(@RequestBody request: DefaultVersionRequest) = {
    val candidate = request.getCandidate
    val version = request.getDefaultVersion
    Option(candidateGenRepo.findByCandidate(candidate)).map { c =>
      Option(versionRepo.findByCandidateAndVersion(c.candidate, version)).map { v =>
        status.Accepted(candidateUpdateRepo.updateDefault(Candidate(v.candidate, v.version)))
      }.getOrElse(throw VersionNotFoundException(candidate, version))
    }.getOrElse(throw CandidateNotFoundException(candidate))
  }

  @ExceptionHandler
  def handle(e: VersionNotFoundException) = status.BadRequest(e.getMessage)

  @ExceptionHandler
  def handle(e: CandidateNotFoundException) = status.BadRequest(e.getMessage)

}

@RestController
class DefaultVersions @Autowired()(val versionRepo: VersionRepo, val candidateGenRepo: CandidateGeneralRepo, val candidateUpdateRepo: CandidateUpdateRepo) extends DefaultVersionController
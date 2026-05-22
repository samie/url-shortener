package com.svenruppert.urlshortener.core.urlmapping.imports;





import java.util.Objects;

public final class ImportResult {
  private  int created;
  private  int skippedConflicts;
  private  int invalid;

  public ImportResult() {
  }

  public int getCreated() {
    return created;
  }

  public void setCreated(int created) {
    this.created = created;
  }

  public int getSkippedConflicts() {
    return skippedConflicts;
  }

  public void setSkippedConflicts(int skippedConflicts) {
    this.skippedConflicts = skippedConflicts;
  }

  public int getInvalid() {
    return invalid;
  }

  public void setInvalid(int invalid) {
    this.invalid = invalid;
  }

  public ImportResult(
      int created,
      int skippedConflicts,
      int invalid
  ) {
    this.created = created;
    this.skippedConflicts = skippedConflicts;
    this.invalid = invalid;
  }

  public int created() {
    return created;
  }

  public int skippedConflicts() {
    return skippedConflicts;
  }

  public int invalid() {
    return invalid;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ImportResult) obj;
    return this.created == that.created &&
        this.skippedConflicts == that.skippedConflicts &&
        this.invalid == that.invalid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(created, skippedConflicts, invalid);
  }

  @Override
  public String toString() {
    return "ImportResult[" +
        "created=" + created + ", " +
        "skippedConflicts=" + skippedConflicts + ", " +
        "invalid=" + invalid + ']';
  }

}
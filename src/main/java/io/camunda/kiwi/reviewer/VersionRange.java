package io.camunda.kiwi.reviewer;

import com.fasterxml.jackson.core.Version;

public sealed interface VersionRange {
  default <T extends VersionRange> T as(Class<T> clazz) {
    return (T) this;
  }

  boolean matches(Version version);

  record Equals(Version version) implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return version.compareTo(this.version) == 0;
    }

    @Override
    public String toString() {
      return "=" + version;
    }
  }

  record Wildcard() implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return true;
    }

    @Override
    public String toString() {
      return "*";
    }
  }

  record Greater(Version version) implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return version.compareTo(this.version) > 0;
    }

    @Override
    public String toString() {
      return ">" + version;
    }
  }

  record Lower(Version version) implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return version.compareTo(this.version) < 0;
    }

    @Override
    public String toString() {
      return "<" + version;
    }
  }

  record GreaterEquals(Version version) implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return version.compareTo(this.version) >= 0;
    }

    @Override
    public String toString() {
      return ">=" + version;
    }
  }

  record LowerEquals(Version version) implements VersionRange {
    @Override
    public boolean matches(Version version) {
      return version.compareTo(this.version) <= 0;
    }

    @Override
    public String toString() {
      return "<=" + version;
    }
  }
}

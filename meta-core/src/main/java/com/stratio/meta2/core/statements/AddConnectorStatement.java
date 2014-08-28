package com.stratio.meta2.core.statements;

import com.stratio.meta.core.metadata.MetadataManager;
import com.stratio.meta.core.utils.Tree;
import com.stratio.meta2.core.engine.validator.Validation;
import com.stratio.meta2.core.engine.validator.ValidationRequirements;

public class AddConnectorStatement extends MetaStatement {

  private String path;

  public AddConnectorStatement(String path){
    this.path = path;
  }

  @Override
  public String toString() {
    return "ADD CONNECTOR "+path;
  }

  @Override
  public String translateToCQL() {
    return null;
  }

  @Override
  public Tree getPlan(MetadataManager metadataManager, String targetKeyspace) {
    return null;
  }

  @Override
  public ValidationRequirements getValidationRequirements() {
    return new ValidationRequirements().add(Validation.MUST_NOT_EXIST_CONNECTOR).
        add(Validation.VALID_CONNECTOR_MANIFEST);
  }
}

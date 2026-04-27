terraform {
  backend "s3" {
    bucket         = "ACCOUNT_ID-cinevision-terraform-state-dev"
    key            = "terraform/dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "cinevision-terraform-locks-dev"
    encrypt        = true
    # profile        = "cinevision-dev"
  }
}

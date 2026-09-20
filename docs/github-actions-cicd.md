# GitHub Actions CI/CD

## V2 deployment target

- Branch: `main`
- Region: `ap-northeast-2`
- ECR repository: `momentlit/server`
- ECS cluster: `momentlit`
- ECS service: `momentlit-server`
- ECS task definition family: `momentlit-momentlit-server`
- Health check: `/actuator/health`

The deploy workflow runs the test suite, builds a `linux/amd64` image tagged with the Git commit SHA, and pushes it to ECR. It then reads the active ECS task definition, replaces the only container image, registers a revision, and waits for ECS to stabilize. This preserves the existing task CPU, memory, roles, networking, log configuration, and ECS-managed secrets.

## Required GitHub configuration

Create a protected GitHub Environment named `production`, then add these environment secrets:

| Name | Value |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | IAM deployment user's access key ID |
| `AWS_SECRET_ACCESS_KEY` | IAM deployment user's secret access key |

Do not add database credentials, JWT secrets, or OpenAI keys to GitHub Actions. ECS task definitions must continue to source runtime secrets from AWS Secrets Manager or SSM Parameter Store.

## IAM access-key policy

Create a dedicated IAM user (for example `github-actions-momentlit-deployer`), generate an access key for it, and attach the following inline policy. `iam:PassRole` is deliberately restricted to the roles already used by the V2 task definition.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuthentication",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "PushServerImage",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:BatchGetImage",
        "ecr:CompleteLayerUpload",
        "ecr:GetDownloadUrlForLayer",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": [
        "arn:aws:ecr:ap-northeast-2:834088498570:repository/momentlit/server",
        "arn:aws:ecr:ap-northeast-2:834088498570:repository/momentlit/chatbot"
      ]
    },
    {
      "Sid": "ManageServerTaskDefinition",
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition"
      ],
      "Resource": "*"
    },
    {
      "Sid": "DeployServerService",
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:UpdateService"
      ],
      "Resource": [
        "arn:aws:ecs:ap-northeast-2:834088498570:service/momentlit/momentlit-server",
        "arn:aws:ecs:ap-northeast-2:834088498570:service/momentlit/momentlit-chatbot"
      ]
    },
    {
      "Sid": "PassOnlyTaskRolesToEcs",
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": [
        "arn:aws:iam::834088498570:role/service-role/ecsTaskExecutionRole",
        "arn:aws:iam::834088498570:role/momentlit-image-task-role",
        "arn:aws:iam::834088498570:role/momentlit-chatbot-task-role"
      ],
      "Condition": {
        "StringEquals": {
          "iam:PassedToService": "ecs-tasks.amazonaws.com"
        }
      }
    }
  ]
}
```

The key must never be committed. Restrict the `production` environment to `main` and rotate or immediately disable the key if it is exposed.

## ChatBot

ChatBot has its own repository and requires an equivalent workflow in that repository. Its values are:

- ECR repository: `momentlit/chatbot`
- ECS service: `momentlit-chatbot`
- Task family: `momentlit-momentlit-chatbot`
- Task role: `momentlit-chatbot-task-role`
- Health check: `/health`

The ChatBot task has a Qdrant sidecar, so its deploy workflow replaces only the `chatbot-api` container image, not every container image. Create the same `production` Environment and the same two access-key secrets in the ChatBot repository; repository-level environment secrets are not shared with the V2 repository.

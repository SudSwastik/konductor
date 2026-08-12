import { Amplify } from "aws-amplify";

const region = process.env.NEXT_PUBLIC_AWS_REGION;
const userPoolId = process.env.NEXT_PUBLIC_COGNITO_USER_POOL_ID;
const userPoolClientId = process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID;

let isConfigured = false;

export function configureAmplify() {
  if (isConfigured) {
    return;
  }

  if (!region || !userPoolId || !userPoolClientId) {
    throw new Error("Missing Cognito environment configuration.");
  }

  Amplify.configure({
    Auth: {
      Cognito: {
        userPoolId,
        userPoolClientId,
      },
    },
  });

  isConfigured = true;
}

export { region, userPoolClientId, userPoolId };

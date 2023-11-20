package com.upemail.registration.service.impl;

import com.upemail.registration.entity.*;
import com.upemail.registration.repository.RegTokenRepository;
import com.upemail.registration.repository.UserRepository;
import com.upemail.registration.security.JwtService;
import com.upemail.registration.service.EmailSenderService;
import com.upemail.registration.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegTokenRepository regTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSenderService emailSenderService;
    private final AuthenticationManager authenticationManager;
    private static final String ACTIVATION_LINK = "http://localhost:8080/user/confirm/?token=";


    @Override
    public ResponseEntity<Result> createUser(RegisterRequest request) {
        Result result = new Result();

        Optional<User> findUser = userRepository.findByEmail(request.getEmail());
        if (findUser.isPresent() && findUser.get().isEnabled()) {
            result.setSuccess(false);
            result.setMessage("email already taken");
            return ResponseEntity.badRequest().body(result);
        }

        /*
        * resend email activation for user registration who's already
        * */

        if (findUser.isPresent() && !findUser.get().isEnabled()) {

            String tokenResend = createRegToken(findUser.get());
            String linkResend = ACTIVATION_LINK + tokenResend;
            emailSenderService.send(request.getEmail(), buildEmail(request.getFullName(), linkResend));

            result.setSuccess(false);
            result.setMessage("you already have an account, email verification is resent");
            result.setData(tokenResend);
            return ResponseEntity.badRequest().body(result);
        }


        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.ROLE_USER);

        User savedUser = userRepository.save(newUser);

        String token = createRegToken(savedUser);

        result.setSuccess(true);
        result.setMessage("user created with id: " + savedUser.getId());
        result.setData("activation token: " + token);

        String link = ACTIVATION_LINK + token;

        emailSenderService.send(request.getEmail(), buildEmail(request.getFullName(), link));

        return ResponseEntity.ok(result);
    }

    public String createRegToken(User user) {
        String token = UUID.randomUUID().toString();
        RegistrationToken regToken = new RegistrationToken();
        regToken.setCreatedAt(LocalDateTime.now());
        regToken.setUser(user);
        regToken.setToken(token);
        regToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        regTokenRepository.save(regToken);

        return token;
    }

    @Override
    public ResponseEntity<Result> userLogin(AuthenticationRequest request) {
        Result result = new Result();

        authenticationManager.authenticate
                (new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        String jwtToken = jwtService.generateToken(user);
        result.setSuccess(true);
        result.setMessage("token created");
        result.setData(jwtToken);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Result> confirmTokenRegistration(String token) {
        Result result = new Result();
        RegistrationToken regToken = regTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("token not found"));

        if (regToken.getConfirmedAt() != null) {
            throw new IllegalStateException("email already confirmed");
        }

        LocalDateTime expiredAt = regToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("token expired");
        }

        regToken.setConfirmedAt(LocalDateTime.now());
        regTokenRepository.save(regToken);

        userRepository.enableUser(regToken.getUser().getEmail());
        result.setSuccess(true);
        result.setMessage("user activated");
        result.setData(null);
        return ResponseEntity.ok(result);
    }

    public String buildEmail(String name, String link) {
        return "<!DOCTYPE html>\n" +
                "<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" lang=\"en\">\n" +
                "  <head>\n" +
                "    <title></title>\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
                "    <!--[if mso]>\n" +
                "\t\t\t\t<xml>\n" +
                "\t\t\t\t\t<o:OfficeDocumentSettings>\n" +
                "\t\t\t\t\t\t<o:PixelsPerInch>96</o:PixelsPerInch>\n" +
                "\t\t\t\t\t\t<o:AllowPNG/>\n" +
                "\t\t\t\t\t</o:OfficeDocumentSettings>\n" +
                "\t\t\t\t</xml>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "    <style>\n" +
                "      * {\n" +
                "        box-sizing: border-box\n" +
                "      }\n" +
                "\n" +
                "      body {\n" +
                "        margin: 0;\n" +
                "        padding: 0\n" +
                "      }\n" +
                "\n" +
                "      a[x-apple-data-detectors] {\n" +
                "        color: inherit !important;\n" +
                "        text-decoration: inherit !important\n" +
                "      }\n" +
                "\n" +
                "      #MessageViewBody a {\n" +
                "        color: inherit;\n" +
                "        text-decoration: none\n" +
                "      }\n" +
                "\n" +
                "      p {\n" +
                "        line-height: inherit\n" +
                "      }\n" +
                "\n" +
                "      .desktop_hide,\n" +
                "      .desktop_hide table {\n" +
                "        mso-hide: all;\n" +
                "        display: none;\n" +
                "        max-height: 0;\n" +
                "        overflow: hidden\n" +
                "      }\n" +
                "\n" +
                "      @media (max-width:720px) {\n" +
                "        .row-content {\n" +
                "          width: 100% !important\n" +
                "        }\n" +
                "\n" +
                "        .mobile_hide {\n" +
                "          display: none\n" +
                "        }\n" +
                "\n" +
                "        .stack .column {\n" +
                "          width: 100%;\n" +
                "          display: block\n" +
                "        }\n" +
                "\n" +
                "        .mobile_hide {\n" +
                "          min-height: 0;\n" +
                "          max-height: 0;\n" +
                "          max-width: 0;\n" +
                "          overflow: hidden;\n" +
                "          font-size: 0\n" +
                "        }\n" +
                "\n" +
                "        .desktop_hide,\n" +
                "        .desktop_hide table {\n" +
                "          display: table !important;\n" +
                "          max-height: none !important\n" +
                "        }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body style=\"background-color:#fff;margin:0;padding:0;-webkit-text-size-adjust:none;text-size-adjust:none\">\n" +
                "    <table class=\"nl-container\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;background-color:#fff\">\n" +
                "      <tbody>\n" +
                "        <tr>\n" +
                "          <td>\n" +
                "            <table class=\"row row-1\" align=\"center\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "              <tbody>\n" +
                "                <tr>\n" +
                "                  <td>\n" +
                "                    <table class=\"row-content stack\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;color:#000;width:700px\" width=\"700\">\n" +
                "                      <tbody>\n" +
                "                        <tr>\n" +
                "                          <td class=\"column column-1\" width=\"100%\" style=\"mso-table-lspace:0;mso-table-rspace:0;font-weight:400;text-align:left;vertical-align:top;padding-top:0;padding-bottom:0;border-top:0;border-right:0;border-bottom:0;border-left:0\">\n" +
                "                            <table class=\"empty_block block-1\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "                              <tr>\n" +
                "                                <td class=\"pad\">\n" +
                "                                  <div></div>\n" +
                "                                </td>\n" +
                "                              </tr>\n" +
                "                            </table>\n" +
                "                          </td>\n" +
                "                        </tr>\n" +
                "                      </tbody>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </tbody>\n" +
                "            </table>\n" +
                "            <table class=\"row row-2\" align=\"center\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "              <tbody>\n" +
                "                <tr>\n" +
                "                  <td>\n" +
                "                    <table class=\"row-content stack\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;background-color:#f6f6f6;color:#000;width:700px\" width=\"700\">\n" +
                "                      <tbody>\n" +
                "                        <tr>\n" +
                "                          <td class=\"column column-1\" width=\"100%\" style=\"mso-table-lspace:0;mso-table-rspace:0;font-weight:400;text-align:left;vertical-align:top;padding-top:40px;padding-bottom:40px;border-top:0;border-right:0;border-bottom:0;border-left:0\">\n" +
                "                            <table class=\"text_block block-1\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;word-break:break-word\">\n" +
                "                              <tr>\n" +
                "                                <td class=\"pad\" style=\"padding-bottom:10px;padding-left:20px;padding-right:20px;padding-top:10px\">\n" +
                "                                  <div style=\"font-family:sans-serif\">\n" +
                "                                    <div class style=\"font-size:12px;mso-line-height-alt:14.399999999999999px;color:#555;line-height:1.2;font-family:Arial,Helvetica Neue,Helvetica,sans-serif\">\n" +
                "                                      <p style=\"margin:0;font-size:14px;text-align:center;mso-line-height-alt:16.8px\">\n" +
                "                                        <strong>\n" +
                "                                          <span style=\"font-size:24px;\">Activate your account</span>\n" +
                "                                        </strong>\n" +
                "                                      </p>\n" +
                "                                    </div>\n" +
                "                                  </div>\n" +
                "                                </td>\n" +
                "                              </tr>\n" +
                "                            </table>\n" +
                "                            <table class=\"text_block block-2\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;word-break:break-word\">\n" +
                "                              <tr>\n" +
                "                                <td class=\"pad\" style=\"padding-bottom:10px;padding-left:30px;padding-right:30px;padding-top:10px\">\n" +
                "                                  <div style=\"font-family:sans-serif\">\n" +
                "                                    <div class style=\"font-size:12px;mso-line-height-alt:18px;color:#555;line-height:1.5;font-family:Arial,Helvetica Neue,Helvetica,sans-serif\">\n" +
                "                                      <p style=\"margin:0;font-size:14px;text-align:center;mso-line-height-alt:21px\">Hi " + name + " <br>Click the button to activate your account, if you have any questions feel free to contact us. </p>\n" +
                "                                    </div>\n" +
                "                                  </div>\n" +
                "                                </td>\n" +
                "                              </tr>\n" +
                "                            </table>\n" +
                "                          </td>\n" +
                "                        </tr>\n" +
                "                      </tbody>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </tbody>\n" +
                "            </table>\n" +
                "            <table class=\"row row-3\" align=\"center\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "              <tbody>\n" +
                "                <tr>\n" +
                "                  <td>\n" +
                "                    <table class=\"row-content stack\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;color:#000;width:700px\" width=\"700\">\n" +
                "                      <tbody>\n" +
                "                        <tr>\n" +
                "                          <td class=\"column column-1\" width=\"100%\" style=\"mso-table-lspace:0;mso-table-rspace:0;font-weight:400;text-align:left;vertical-align:top;padding-top:5px;padding-bottom:40px;border-top:0;border-right:0;border-bottom:0;border-left:0\">\n" +
                "                            <table class=\"button_block block-1\" width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "                              <tr>\n" +
                "                                <td class=\"pad\">\n" +
                "                                  <div class=\"alignment\" align=\"center\">\n" +
                "                                    <!--[if mso]>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<v:roundrect\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\txmlns:v=\"urn:schemas-microsoft-com:vml\"\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\txmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"https://portal.enginemailer.com/Account/Register\" style=\"height:48px;width:142px;v-text-anchor:middle;\" arcsize=\"105%\" stroke=\"false\" fillcolor=\"#3AAEE0\">\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<w:anchorlock/>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<v:textbox inset=\"0px,0px,0px,0px\">\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<center style=\"color:#ffffff; font-family:Arial, sans-serif; font-size:16px\">\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<![endif]-->\n" +
                "                                    <a href=\"" + link + "\" target=\"_blank\" style=\"text-decoration:none;display:inline-block;color:#ffffff;background-color:#3AAEE0;border-radius:50px;width:auto;border-top:0px solid transparent;font-weight:undefined;border-right:0px solid transparent;border-bottom:0px solid transparent;border-left:0px solid transparent;padding-top:8px;padding-bottom:8px;font-family:Arial, Helvetica Neue, Helvetica, sans-serif;font-size:16px;text-align:center;mso-border-alt:none;word-break:keep-all;\">\n" +
                "                                      <span style=\"padding-left:40px;padding-right:40px;font-size:16px;display:inline-block;letter-spacing:normal;\">\n" +
                "                                        <span dir=\"ltr\" style=\"word-break: break-word; line-height: 32px;\">\n" +
                "                                          <strong>Activate</strong>\n" +
                "                                        </span>\n" +
                "                                      </span>\n" +
                "                                    </a>\n" +
                "                                    <!--[if mso]>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</center>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</v:textbox>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</v:roundrect>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<![endif]-->\n" +
                "                                  </div>\n" +
                "                                </td>\n" +
                "                              </tr>\n" +
                "                            </table>\n" +
                "                          </td>\n" +
                "                        </tr>\n" +
                "                      </tbody>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </tbody>\n" +
                "            </table>\n" +
                "            <table class=\"row row-4\" align=\"center\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "              <tbody>\n" +
                "                <tr>\n" +
                "                  <td>\n" +
                "                    <table class=\"row-content stack\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0;color:#000;width:700px\" width=\"700\">\n" +
                "                      <tbody>\n" +
                "                        <tr>\n" +
                "                          <td class=\"column column-1\" width=\"100%\" style=\"mso-table-lspace:0;mso-table-rspace:0;font-weight:400;text-align:left;vertical-align:top;padding-top:25px;padding-bottom:25px;border-top:0;border-right:0;border-bottom:0;border-left:0\">\n" +
                "                            <table class=\"empty_block block-1\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"mso-table-lspace:0;mso-table-rspace:0\">\n" +
                "                              <tr>\n" +
                "                                <td class=\"pad\">\n" +
                "                                  <div></div>\n" +
                "                                </td>\n" +
                "                              </tr>\n" +
                "                            </table>\n" +
                "                          </td>\n" +
                "                        </tr>\n" +
                "                      </tbody>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </tbody>\n" +
                "            </table>\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "      </tbody>\n" +
                "    </table>\n" +
                "    <!-- End -->\n" +
                "    <div style=\"background-color:transparent;\"></div>\n" +
                "  </body>\n" +
                "</html>";
    }
}

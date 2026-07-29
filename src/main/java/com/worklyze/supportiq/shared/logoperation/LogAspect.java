package com.worklyze.supportiq.shared.logoperation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.time.temporal.Temporal;

@Aspect
@Component
@Slf4j
public class LogAspect {

    private final ObjectMapper mapper = new ObjectMapper();

    public LogAspect() {
        // uso o objectMapper para remover o atributo com @LogSensitive
        this.mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public boolean hasIgnoreMarker(AnnotatedMember m) {
                return m.hasAnnotation(LogSensitive.class) || super.hasIgnoreMarker(m);
            }
        });
    }

    @Around("@annotation(logOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();
        String[] paramNames = signature.getParameterNames();

        Object[] args = joinPoint.getArgs();
        StringBuilder sb = new StringBuilder();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();

        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();

        adicionarInputs(args, paramAnnotations, sb, paramNames);

        log.info("HTTP: {} | controller={} | method={} | operation={} | user={} | params={}",
                httpMethod,
                signature.getDeclaringType().getSimpleName(),
                signature.getMethod().getName(),
                uri,
                getUsernameSafe(),
                sb.toString());

        return joinPoint.proceed();
    }

    private void adicionarInputs(Object[] args, Annotation[][] paramAnnotations, StringBuilder sb, String[] paramNames) {
        boolean first = true;

        for (int i = 0; i < args.length; i++) {

            Annotation[] annotations = paramAnnotations[i];
            Object arg = args[i];

            if (arg == null) continue;

            // NUNCA tenta serializar request/response/multipart - isso aciona
            // getWriter()/getInputStream() por baixo dos panos e quebra a escrita
            // real do body depois pelo Spring MVC (IllegalStateException:
            // getWriter() has already been called for this response)
            if (isNaoLogavel(arg.getClass())) {
                continue;
            }

            boolean isRequestBody = hasAnnotation(annotations, RequestBody.class);
            boolean isModelAttribute = hasAnnotation(annotations, ModelAttribute.class);
            boolean isRequestPart = hasAnnotation(annotations, RequestPart.class);
            boolean probablyPojo = !isSimpleType(arg.getClass());

            String serialized;

            try {
                if (isRequestBody || isModelAttribute || isRequestPart || probablyPojo) {
                    serialized = toJsonSafe(arg);
                } else {
                    serialized = String.valueOf(arg);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                serialized = "\"<error_serializing>\"";
            }

            if (!first) sb.append(", ");
            sb.append(paramLabel(i, paramNames)).append(serialized);
            first = false;
        }
    }

    private String paramLabel(int index, String[] paramNames) {
        if (paramNames != null &&
                index < paramNames.length &&
                paramNames[index] != null) {
            return paramNames[index] + "=";
        }
        return "arg" + index + "=";
    }

    private boolean isSimpleType(Class<?> cl) {
        return cl.isPrimitive()
                || CharSequence.class.isAssignableFrom(cl)
                || Number.class.isAssignableFrom(cl)
                || Boolean.class.isAssignableFrom(cl)
                || java.util.Date.class.isAssignableFrom(cl)
                || Temporal.class.isAssignableFrom(cl)
                || Enum.class.isAssignableFrom(cl);
    }

    /**
     * Tipos que nunca devem ser logados/serializados: request, response e
     * upload de arquivo. Serializar esses objetos via Jackson dispara getters
     * como getWriter()/getInputStream(), com efeitos colaterais graves sobre
     * o ciclo de vida da requisição/resposta.
     */
    private boolean isNaoLogavel(Class<?> cl) {
        return ServletRequest.class.isAssignableFrom(cl)
                || ServletResponse.class.isAssignableFrom(cl)
                || MultipartFile.class.isAssignableFrom(cl);
    }

    private String toJsonSafe(Object obj) {
        if (obj == null) return "null";

        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private boolean hasAnnotation(Annotation[] anns, Class<? extends Annotation> type) {
        if (anns == null) return false;

        for (Annotation a : anns) {
            if (a.annotationType().equals(type)) return true;
        }
        return false;
    }

    private String getUsernameSafe() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return "anonymous";
            return String.valueOf(auth.getName());
        } catch (Exception e) {
            return "unknown";
        }
    }
}
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.build;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import static org.glassfish.build.utils.MavenHelper.createArtifact;
import static org.glassfish.build.utils.MavenHelper.createAttachedArtifacts;
import static org.glassfish.build.utils.MavenHelper.getPomInTarget;
import static org.glassfish.build.utils.MavenHelper.readModel;

/**
 * Guess artifacts from target directory and attach them to the project.
 */
@Mojo(name = "attach-all-artifacts",
      requiresOnline = true)
public final class AttachAllArtifactsMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The project pom file.
     */
    @Parameter(property = "attach.all.artifacts.pomFile",
            defaultValue = "${project.file}",
            required = true)
    private File pomFile;

    /**
     * Skip this mojo.
     */
    @Parameter(property = "attach.all.artifacts.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping artifact attachment");
            return;
        }

        // check for an existing .pom under target
        File targetPom = getPomInTarget(project.getBuild().getDirectory());
        if (targetPom != null) {
            pomFile = targetPom;
        }

        // if supplied pomFile is invalid, default to the project's pom
        if (pomFile == null || !pomFile.exists()) {
            pomFile = project.getFile();
        }

        if (!pomFile.exists()) {
            getLog().info("Skipping as there is no model to read from");
            return;
        }

        // read the model manually
        Model model = readModel(pomFile);

        // create the project artifact manually
        Artifact artifact = createArtifact(project.getBuild().getDirectory(),
                model);
        if (artifact == null) {
            getLog().info(
                    "Skipping as there is no file found for this artifact");
            return;
        }

        // create the project attached artifacts manually
        List<Artifact> attachedArtifacts =
                createAttachedArtifacts(project.getBuild().getDirectory(),
                        artifact, model);

        // add metadata to the project if not a "pom" type
        if (!"pom".equals(model.getPackaging())) {
            ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
                    pomFile);
            artifact.addMetadata(metadata);
        }

        // set main artifact
        project.setArtifact(artifact);

        // set model
        project.setFile(pomFile);

        for (Iterator i = attachedArtifacts.iterator(); i.hasNext();) {
            project.addAttachedArtifact((Artifact) i.next());
        }
    }
}

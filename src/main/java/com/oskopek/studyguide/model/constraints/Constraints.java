package com.oskopek.studyguide.model.constraints;

import com.oskopek.studyguide.constraint.*;
import com.oskopek.studyguide.model.CourseEnrollment;
import com.oskopek.studyguide.model.SemesterPlan;
import com.oskopek.studyguide.model.courses.Course;
import com.oskopek.studyguide.weld.BeanManagerUtil;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * A set of {@link CourseGroup}s and their {@link Constraint}s, along with global constraints.
 */
public class Constraints {

    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    private final ListProperty<CourseGroupConstraint> courseGroupConstraintList;
    private final ListProperty<GlobalConstraint> globalConstraintList;
    private final ListProperty<CourseEnrollmentConstraint> courseEnrollmentConstraintList;

    /**
     * Initialize an empty set of constraints.
     */
    public Constraints() {
        courseGroupConstraintList = new SimpleListProperty<>(FXCollections.observableArrayList());
        globalConstraintList = new SimpleListProperty<>(FXCollections.observableArrayList());
        courseEnrollmentConstraintList = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    /**
     * Get the list of {@link CourseGroup} constraints.
     *
     * @return non-null
     */
    public ObservableList<CourseGroupConstraint> getCourseGroupConstraintList() {
        return courseGroupConstraintList.get();
    }

    /**
     * Private setter for Jackson persistence.
     *
     * @param courseGroupConstraintList the list of {@link CourseGroupConstraint}s to set
     */
    private void setCourseGroupConstraintList(List<CourseGroupConstraint> courseGroupConstraintList) {
        this.courseGroupConstraintList.set(FXCollections.observableArrayList(courseGroupConstraintList));
    }

    /**
     * Get the list property of {@link CourseGroup} constraints.
     *
     * @return non-null
     */
    public ListProperty<CourseGroupConstraint> courseGroupConstraintListProperty() {
        return courseGroupConstraintList;
    }

    /**
     * Get the list of global constraints.
     *
     * @return non-null
     */
    public ObservableList<GlobalConstraint> getGlobalConstraintList() {
        return globalConstraintList.get();
    }

    /**
     * Private setter for Jackson persistence.
     *
     * @param globalConstraintList the list of {@link GlobalConstraint}s to set
     */
    private void setGlobalConstraintList(List<GlobalConstraint> globalConstraintList) {
        this.globalConstraintList.set(FXCollections.observableArrayList(globalConstraintList));
    }

    /**
     * Get the list property of global constraints.
     *
     * @return non-null
     */
    public ListProperty<GlobalConstraint> globalConstraintListProperty() {
        return globalConstraintList;
    }

    /**
     * Get the list of {@link com.oskopek.studyguide.model.CourseEnrollment} constraints.
     *
     * @return non-null
     */
    public ObservableList<CourseEnrollmentConstraint> getCourseEnrollmentConstraintList() {
        return courseEnrollmentConstraintList.get();
    }

    /**
     * Private setter for Jackson persistence.
     *
     * @param courseEnrollmentConstraintList the list of {@link CourseEnrollmentConstraint}s to set
     */
    private void setCourseEnrollmentConstraintList(List<CourseEnrollmentConstraint> courseEnrollmentConstraintList) {
        this.courseEnrollmentConstraintList.set(FXCollections.observableArrayList(courseEnrollmentConstraintList));
    }

    /**
     * Get the list property of {@link com.oskopek.studyguide.model.CourseEnrollment} constraints.
     *
     * @return non-null
     */
    public ListProperty<CourseEnrollmentConstraint> courseEnrollmentConstraintListProperty() {
        return courseEnrollmentConstraintList;
    }

    /**
     * Removes all {@link CourseEnrollmentConstraint}s that restrict the given course enrollment.
     *
     * @param courseEnrollment the course enrollment
     */
    public void removeAllCourseEnrollmentConstraints(CourseEnrollment courseEnrollment) {
        removeAllCourseEnrollmentConstraints(Collections.singleton(courseEnrollment));
    }

    /**
     * Removes all {@link CourseEnrollmentConstraint}s that restrict the given course enrollments.
     *
     * @param courseEnrollments the collection of course enrollments whose constraints to remove
     */
    public void removeAllCourseEnrollmentConstraints(Collection<CourseEnrollment> courseEnrollments) {
        Set<CourseEnrollment> courseEnrollmentSet = new HashSet<>(courseEnrollments);
        List<CourseEnrollmentConstraint> courseEnrollmentConstraintListCopy = new ArrayList<>(
                courseEnrollmentConstraintList.size() - courseEnrollmentSet.size() * 2);
        courseEnrollmentConstraintList.stream().filter(cec -> !courseEnrollmentSet.contains(cec.getCourseEnrollment()))
                .forEach(courseEnrollmentConstraintListCopy::add);
        setCourseEnrollmentConstraintList(courseEnrollmentConstraintListCopy);
        recheckAll();
    }

    /**
     * Adds all applicable {@link CourseEnrollmentConstraint}s that should restrict the given course enrollment.
     *
     * @param courseEnrollment the course enrollment
     * @param semesterPlan the semesterPlan to set into them
     */
    public void addAllCourseEnrollmentConstraints(CourseEnrollment courseEnrollment, SemesterPlan semesterPlan) {
        CourseEnrollmentConstraint c1 = BeanManagerUtil.createBeanInstance(CourseEnrollmentCorequisiteConstraint.class);
        c1.setCourseEnrollment(courseEnrollment);
        c1.setSemesterPlan(semesterPlan);

        CourseEnrollmentConstraint c2 = BeanManagerUtil
                .createBeanInstance(CourseEnrollmentPrerequisiteConstraint.class);
        c2.setCourseEnrollment(courseEnrollment);
        c2.setSemesterPlan(semesterPlan);

        CourseEnrollmentConstraint c3 = BeanManagerUtil.createBeanInstance(CourseEnrollmentEnrolledInConstraint.class);
        c3.setCourseEnrollment(courseEnrollment);
        c3.setSemesterPlan(semesterPlan);

        courseEnrollmentConstraintList.addAll(c1, c2, c3);
    }

    /**
     * Constructs a stream of all the constraints (of all types) contained in this object.
     *
     * @return a stream of constraints
     */
    public Stream<DefaultConstraint> allConstraintStream() {
        return Stream.concat(Stream
                        .concat(getCourseEnrollmentConstraintList().stream(), getCourseGroupConstraintList().stream()),
                getGlobalConstraintList().stream());
    }

    /**
     * Recheck all constraints because a course enrollment changed in the model.
     *
     * @param enrollment the course enrollment that caused this recheck
     * @see #recheckAll()
     */
    public void recheckAll(CourseEnrollment enrollment) {
        logger.debug("Rechecking all because of an enrollment change: {}", enrollment);
        recheckAll();
    }

    /**
     * Recheck all constraints because a course changed in the model.
     *
     * @param course the course that caused this recheck
     * @see #recheckAll()
     */
    public void recheckAll(Course course) {
        logger.debug("Rechecking all because of a course change: {}", course);
        recheckAll();
    }

    /**
     * Recheck all constraints and fire appropriate events.
     */
    public void recheckAll() {
        logger.debug("Rechecking constraints...");
        allConstraintStream().forEach(Constraint::validate);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getCourseGroupConstraintList()).append(getGlobalConstraintList())
                .append(getCourseEnrollmentConstraintList()).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Constraints)) {
            return false;
        }
        Constraints that = (Constraints) o;
        return new EqualsBuilder().append(getCourseGroupConstraintList(), that.getCourseGroupConstraintList())
                .append(getGlobalConstraintList(), that.getGlobalConstraintList())
                .append(getCourseEnrollmentConstraintList(), that.getCourseEnrollmentConstraintList()).isEquals();
    }

    @Override
    public String toString() {
        return "Constraints[" + courseEnrollmentConstraintList.size() + ", " + courseGroupConstraintList.size() + ", "
                + globalConstraintList.size() + "]";
    }
}
